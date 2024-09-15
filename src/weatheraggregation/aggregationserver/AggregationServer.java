package weatheraggregation.aggregationserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import weatheraggregation.core.*;
import weatheraggregation.jsonparser.CustomJsonParser;
import weatheraggregation.jsonparser.CustomParseException;

public class AggregationServer {

    public static final int DEFAULT_PORT = 4567;
    public static final int PURGE_SECONDS = 30;

    private final String contentFilename;
    private final LamportClock lamportClock;
    private final int serverPort;
    private final boolean testing;

    private final Object requestLock = false;
    private HttpServer server;
    private ScheduledExecutorService scheduler;

    public AggregationServer(String content_filename) {
        this(content_filename, DEFAULT_PORT);
    }

    public AggregationServer(String contentFilename, int serverPort) {
        this(contentFilename, serverPort, false);
    }

    public AggregationServer(String contentFilename, int serverPort, boolean testing) {
        this.contentFilename = contentFilename;
        this.lamportClock = new LamportClockImpl();
        this.serverPort = serverPort;
        this.testing = testing;

        // Try creating the file if it doesn't exist
        FileHelpers.tryCreateFile(contentFilename);
    }

    /**
     * Start the AggregationServer, listening out for HTTP requests from clients.
     * Also, purge outdated weather data on a regular schedule.
     */
    public void startServer() {
        try {
            // Create a localhost with the desired port
            InetAddress localhost = InetAddress.getLocalHost();
            InetSocketAddress socketAddress = new InetSocketAddress(localhost, this.serverPort);

            // Create the server, mounted on localhost
            this.server = HttpServer.create(socketAddress, 0);
            this.server.createContext("/", this::handleRequest);

            this.server.setExecutor(null);
            this.server.start();

            System.out.println("Server " + this.server.getAddress() + " started.");

            // Only purge outdated data if we aren't testing
            if (!testing) startMaintenanceLoop();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoke the appropriate handler for an HTTP request.
     * @param exchange An object containing the HTTP exchange.
     */
    private void handleRequest(HttpExchange exchange) {
        String method = exchange.getRequestMethod();

        synchronized (this.requestLock) {
            boolean result;
            switch (method) {
                case "GET" -> result = this.handleGET(exchange);
                case "PUT" -> result = this.handlePUT(exchange);
                default -> result = this.handleMiscellaneous(exchange);
            }
            if (!result) System.err.println("Failed to send response for request with method " + method);
        }
    }

    /**
     * Schedule to purge outdated weather data every PURGE_SECONDS seconds.
     */
    private void startMaintenanceLoop() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Purging outdated data...");
            int realTime = (int) Instant.now().getEpochSecond();
            try {
                FileHelpers.expungeAndSwapWeatherFile(this.contentFilename, realTime);
            } catch (IOException e) {
                System.err.println("IO exception when expunging data: " + e.getMessage());
            }
        }, 0, PURGE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Handle a message that is not a GET or a PUT by responding with a 400 status code.
     * @param exchange An object containing the HTTP exchange.
     * @return Whether the response successfully sent.
     */
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

    /**
     * Handle a GET request, responding with the requested weather data.
     * @param exchange An object containing the HTTP exchange.
     * @return Whether the response successfully sent.
     */
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
                jsonString = FileHelpers.readWeatherFileFirst(this.contentFilename);
            } else {
                jsonString = FileHelpers.readWeatherFile(this.contentFilename, stationId);
            }

            if (jsonString != null) {
                exchange.sendResponseHeaders(200, jsonString.getBytes().length);
                try (OutputStream outputStream = exchange.getResponseBody()) {
                    outputStream.write(jsonString.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
            return true;

        } catch (CustomParseException e) {
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

    /**
     * Handle a PUT request, committing the received weather data to memory.
     * @param exchange An object containing the HTTP exchange.
     * @return Whether the response successfully sent.
     */
    private boolean handlePUT(HttpExchange exchange) {
        System.out.println("Handling PUT...");
        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int eventTime = Integer.parseInt(headers.getOrDefault("Lamport-time", "0"));
        this.lamportClock.processEvent(eventTime);
        exchange.getResponseHeaders().add("Lamport-time", String.valueOf(this.lamportClock.getLamportTime()));

        // Send a 200 OK response
        try {
            String weatherString = ConversionHelpers.inputStreamToString(exchange.getRequestBody());
            if (weatherString.length() == 3) {
                exchange.sendResponseHeaders(204, -1);
                return false;
            }

            Map<String, String> weatherJson = CustomJsonParser.stringToJson(weatherString);
            if (!weatherJson.containsKey("id")) {
                exchange.sendResponseHeaders(500, -1);
                return false;
            }

            String stationId = weatherJson.get("id");

            // Try to commit this data to memory
            try {
                int realTime = (int) Instant.now().getEpochSecond();
                boolean replaced = FileHelpers.writeAndSwapWeatherFile(this.contentFilename, stationId, realTime, this.lamportClock.getLamportTime(), weatherString);

                if (replaced) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(201, -1);
                }
                return true;

            } catch (IOException e) {
                System.err.println("Commit failed, retrying...");
            }
            return false;

        } catch (IOException e) {
            System.err.println("IO Exception when sending OK: " + e.getMessage());
        } catch (CustomParseException e) {
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

    /**
     * Shut down the HTTP server and scheduled maintenance loop.
     */
    public void shutdownServer() {
        // Shut down the HTTP server
        if (server != null) {
            System.out.println("Shutting down the HTTP server...");
            server.stop(0);
        }

        // Shut down the scheduled maintenance service
        if (scheduler != null && !scheduler.isShutdown()) {
            System.out.println("Shutting down the scheduler...");
            scheduler.shutdownNow();
        }
    }

    /**
     * The entry point for the server.
     * @param args Command-line arguments.
     */
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