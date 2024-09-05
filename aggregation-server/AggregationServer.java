import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregationServer {

    public static final int DEFAULT_PORT = 4567;

    private final LamportClock lamportClock;
    private final int serverPort;

    private final JSONParser jsonParser;

    public AggregationServer() {
        this(DEFAULT_PORT);
    }

    public AggregationServer(int serverPort) {
        this.lamportClock = new LamportClockImpl();
        this.serverPort = serverPort;
        this.jsonParser = new JSONParser();
    }

    void startServer() {
        try {
            // Create a localhost with the desired port
            InetAddress localhost = InetAddress.getLocalHost();
            InetSocketAddress socketAddress = new InetSocketAddress(localhost, this.serverPort);

            // Create the server, mounted on localhost
            HttpServer server = HttpServer.create(socketAddress, 0);
            server.createContext("/", this::handleRequest);

            // Start the server
            server.setExecutor(null);
            server.start();

            System.out.println("Server " + server.getAddress() + " started.");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Method to handle HTTP requests
    private void handleRequest(HttpExchange exchange) throws IOException {
        // Get the request method
        String method = exchange.getRequestMethod();

        switch (method) {
            case "GET" -> this.handleGET(exchange);
            case "PUT" -> this.handlePUT(exchange);
        }
    }

    private void handleGET(HttpExchange exchange) throws IOException {
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
            String value = String.join(", ", header.getValue());
            headers.put(header.getKey(), value);
        }

        // Process event with the lamport clock
        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-Time", "0"));
        this.lamportClock.processEvent(otherTime);

        try (FileReader reader = new FileReader("aggregation-server/weather_data.txt")) {
            JSONObject jsonObject = (JSONObject) this.jsonParser.parse(reader);
            String jsonString = jsonObject.toJSONString();

            exchange.sendResponseHeaders(200, jsonString.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(jsonString.getBytes());
            os.close();

        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    private String readRequestBody(InputStream inputStream) throws IOException {
        // Convert InputStream to String
        StringBuilder stringBuilder = new StringBuilder();
        int i;
        while ((i = inputStream.read()) != -1) {
            stringBuilder.append((char) i);
        }
        return stringBuilder.toString();
    }

    private void handlePUT(HttpExchange exchange) {
        // Extract headers
        Map<String, String> headers = new HashMap<>();
        for (Map.Entry<String, List<String>> header : exchange.getRequestHeaders().entrySet()) {
            String value = String.join(", ", header.getValue());
            headers.put(header.getKey(), value);
        }

        // Process event with the lamport clock
        int otherTime = Integer.parseInt(headers.getOrDefault("Lamport-Time", "0"));
        this.lamportClock.processEvent(otherTime);

        try {

            String requestBody = readRequestBody(exchange.getRequestBody());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("aggregation-server/weather_data.txt"))) {
                writer.write(requestBody);
            }
            exchange.sendResponseHeaders(200, 0);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // Create an AggregationServer
        AggregationServer server;

        // Construct the server with optional command-line arguments
        if (args.length > 0) {
            int hostPort = Integer.parseInt(args[0]);
            server = new AggregationServer(hostPort);
        } else {
            server = new AggregationServer();
        }

        // Run the server
        server.startServer();
    }
}