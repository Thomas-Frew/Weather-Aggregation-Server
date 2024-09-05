import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;

public class AggregationServer {

    public static final int DEFAULT_PORT = 4567;

    private final LamportClock lamportClock;
    private final int serverPort;

    public AggregationServer() {
        this(DEFAULT_PORT);
    }

    public AggregationServer(int serverPort) {
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

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to handle HTTP requests
    private void handleRequest(HttpExchange exchange) {
        String method = exchange.getRequestMethod();

        boolean result;
        switch (method) {
            case "GET" -> result = this.handleGET(exchange);
            case "PUT" -> result = this.handlePUT(exchange);
            default -> result = this.handleMiscellaneous(exchange);
        }

        if (!result) System.out.println("Request handler for " + method + " failed");
    }

    private boolean handleMiscellaneous(HttpExchange exchange) {
        System.out.println("Handling Miscellaneous Message...");

        // Send a 400 Bad Request
        try {
            exchange.sendResponseHeaders(400, -1);
            return true;
        } catch (IOException e) {
            System.err.println("IO Exception when sending 400 response: " + e.getMessage());
        }

        return false;
    }

    private boolean handleGET(HttpExchange exchange) {
        System.out.println("Handling GET...");

        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-Time", "0"));
        this.lamportClock.processEvent(otherTime);
        exchange.getResponseHeaders().add("Lamport-Time", String.valueOf(this.lamportClock.getLamportTime()));

        // Send a 200 OK response
        try {
            String jsonString = ConversionHelpers.readJSONFile("aggregation-server/weather_data.txt");
            exchange.sendResponseHeaders(200, jsonString.getBytes().length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(jsonString.getBytes());
            }

            return true;

        } catch (ParseException e) {
            System.err.println("Parse exception: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Exception when sending OK response: " + e.getMessage());
        }

        // Send a 500 Internal Server Error response
        try {
            exchange.sendResponseHeaders(500, -1);
        } catch (IOException e) {
            System.err.println("IO Exception when sending error response: " + e.getMessage());
        }

        return false;
    }

    private boolean handlePUT(HttpExchange exchange) {
        System.out.println("Handling PUT...");
        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-Time", "0"));
        this.lamportClock.processEvent(otherTime);
        exchange.getResponseHeaders().add("Lamport-Time", String.valueOf(this.lamportClock.getLamportTime()));

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

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("aggregation-server/weather_data.txt"))) {
                writer.write(weatherString);
            }
            exchange.sendResponseHeaders(200, -1);
            return true;

        } catch (IOException e) {
            System.err.println("IO Exception when sending OK response: " + e.getMessage());
        } catch (ParseException e) {
            System.err.println("IO Exception when parsing request JSON: " + e.getMessage());
        }

        // Send a 500 Internal Server Error response
        // Catches malformed JSON and failed sending
        try {
            exchange.sendResponseHeaders(500, -1);
        } catch (IOException e) {
            System.err.println("IO Exception when sending error response: " + e.getMessage());
        }

        return false;
    }

    public static void main(String[] args) {
        AggregationServer server;

        if (args.length > 0) {
            int hostPort = Integer.parseInt(args[0]);
            server = new AggregationServer(hostPort);
        } else {
            server = new AggregationServer();
        }

        server.startServer();
    }
}