import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AggregationServer {

    public static final int DEFAULT_PORT = 4567;
    public static final int MAX_COMMIT_ATTEMPTS = 10;

    private final String contentFilename;

    private final LamportClock lamportClock;
    private final int serverPort;

    private final Object requestLock = false;

    public AggregationServer(String content_filename) {
        this(content_filename, DEFAULT_PORT);
    }

    public AggregationServer(String contentFilename, int serverPort) {
        this.contentFilename = contentFilename;
        this.lamportClock = new LamportClockImpl();
        this.serverPort = serverPort;
    }

    void startServer() {
        try {
            // Create a localhost with the desired port
            InetAddress localhost = InetAddress.getLocalHost();
            InetSocketAddress socketAddress = new InetSocketAddress(localhost, this.serverPort);

            // Create the server, mounted on localhost
            HttpServer server = HttpServer.create(socketAddress, 0);
            server.createContext("/", this::handleRequest);

            server.setExecutor(null);
            server.start();

            System.out.println("Server " + server.getAddress() + " started.");

            startMaintenanceLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to handle HTTP requests
    private void handleRequest(HttpExchange exchange) {
        String method = exchange.getRequestMethod();

        synchronized (this.requestLock) {
            boolean result;
            switch (method) {
                case "GET" -> result = this.handleGET(exchange);
                case "PUT" -> result = this.handlePUT(exchange);
                default -> result = this.handleMiscellaneous(exchange);
            }
            if (!result) System.err.println("Request handler for " + method + " failed");
        }
    }

    // Method to expunge outdated weather data
    private void startMaintenanceLoop() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Purging outdated data...");
            int realTime = (int) Instant.now().getEpochSecond();
            try {
                FileHelpers.expungeAndSwapWeatherFile(this.contentFilename, realTime);
            } catch (IOException e) {
                System.err.println("IO exception when expunging data: " + e.getMessage());
            }
        }, 0, 30, TimeUnit.SECONDS);
    }

    private boolean handleMiscellaneous(HttpExchange exchange) {
        System.out.println("Handling Miscellaneous Message...");

        // Send a 400 Bad Request
        try {
            exchange.sendResponseHeaders(400, -1);
            return true;
        } catch (IOException e) {
            System.err.println("IO Exception when sending error: " + e.getMessage());
        }
        return false;
    }

    private boolean handleGET(HttpExchange exchange) {
        System.out.println("Handling GET...");

        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-time", "0"));
        this.lamportClock.processEvent(otherTime);
        exchange.getResponseHeaders().add("Lamport-time", String.valueOf(this.lamportClock.getLamportTime()));

        // Send a 200 OK response
        try {
            String stationId = headers.getOrDefault("Station-id", null);
            String jsonString;

            if (stationId == null) {
                jsonString = FileHelpers.readWeatherFile(this.contentFilename);
            } else {
                jsonString = FileHelpers.readWeatherFile(this.contentFilename, stationId);
            }

            if (jsonString != null) {
                exchange.sendResponseHeaders(200, jsonString.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(jsonString.getBytes());
                }
                return true;
            } else {
                exchange.sendResponseHeaders(404, -1);
                return false;
            }

        } catch (ParseException e) {
            System.err.println("Parse exception: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Exception when sending response: " + e.getMessage());
        }

        // Send a 500 Internal Server Error response
        try {
            exchange.sendResponseHeaders(500, -1);
        } catch (IOException e) {
            System.err.println("IO Exception when sending error: " + e.getMessage());
        }

        return false;
    }

    private boolean handlePUT(HttpExchange exchange) {
        System.out.println("Handling PUT...");
        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int eventTime = Integer.parseInt(headers.getOrDefault("Lamport-time", "0"));
        this.lamportClock.processEvent(eventTime);
        exchange.getResponseHeaders().add("Lamport-time", String.valueOf(this.lamportClock.getLamportTime()));

        // Send a 200 OK response
        try {
            String weatherString = ConversionHelpers.requestBodyToString(exchange.getRequestBody());
            if (weatherString.length() == 3) {
                exchange.sendResponseHeaders(204, -1);
                return false;
            }

            JSONObject weatherJson = ConversionHelpers.stringToJSON(weatherString);
            if (!weatherJson.containsKey("id")) {
                exchange.sendResponseHeaders(500, -1);
                return false;
            }

            String stationId = weatherJson.get("id").toString();

            // Try to commit this data to memory
            int commitAttempts = 0;
            while (commitAttempts < MAX_COMMIT_ATTEMPTS) {
                try {
                    int realTime = (int) Instant.now().getEpochSecond();
                    boolean replaced = FileHelpers.writeAndSwapWeatherFile(this.contentFilename, stationId, realTime, eventTime, weatherString);

                    if (replaced) {
                        exchange.sendResponseHeaders(200, -1);
                    } else {
                        exchange.sendResponseHeaders(201, -1);
                    }
                    return true;

                } catch (IOException e) {
                    System.err.println("Commit failed, retrying...");
                    commitAttempts++;
                }
            }
            return false;

        } catch (IOException e) {
            System.err.println("IO Exception when sending OK: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("Parse exception: " + e.getMessage());
        }

        // Send a 500 Internal Server Error response
        // Catches malformed JSON and failed sending
        try {
            exchange.sendResponseHeaders(500, -1);
        } catch (IOException e) {
            System.err.println("IO Exception when sending error: " + e.getMessage());
        }

        return false;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java AggregationServer <content_filename> <port>?");
            return;
        }

        AggregationServer server;
        if (args.length > 1) {
            int hostPort = Integer.parseInt(args[1]);
            server = new AggregationServer(args[0], hostPort);
        } else {
            server = new AggregationServer(args[0]);
        }

        server.startServer();
    }
}