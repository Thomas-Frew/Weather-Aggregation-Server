import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
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
            default -> result = false;
        }

        if (!result) System.out.println("Request handler for " + method + " failed");
    }

    private boolean handleGET(HttpExchange exchange) {
        System.out.println("Handling GET...");

        Map<String, String> headers = ConversionHelpers.requestHeadersToMap(exchange.getRequestHeaders());

        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-Time", "0"));
        this.lamportClock.processEvent(otherTime);
        exchange.getResponseHeaders().add("Lamport-Time", String.valueOf(this.lamportClock.getLamportTime()));

        try {
            String jsonString = ConversionHelpers.readJSONFile("aggregation-server/weather_data.txt");

            // Send a 200 OK response with the content length of the JSON string
            exchange.sendResponseHeaders(200, jsonString.getBytes().length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(jsonString.getBytes());
            }

            return true;

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
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

        try {
            String requestBody = ConversionHelpers.requestBodyToString(exchange.getRequestBody());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("aggregation-server/weather_data.txt"))) {
                writer.write(requestBody);
            }

            // Send a 200 OK response
            exchange.sendResponseHeaders(200, -1);

            return true;

        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + e.getMessage());
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