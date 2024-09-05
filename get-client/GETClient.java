import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.net.*;
import java.io.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;

public class GETClient implements AggregationClient {

    LamportClock lamportClock;
    public final String serverHostname;
    public final String stationId;

    private final URI serverURI;
    private HttpClient httpClient;

    private final JSONParser jsonParser;

    public GETClient(String serverHostname) {
        this(serverHostname, null);
    }

    public GETClient(String serverHostname, String stationId) {
        this.lamportClock = new LamportClockImpl();
        this.serverHostname = serverHostname;
        this.stationId = stationId;

        // Establish permanent connection URI
        String uri = "http://" + this.serverHostname;
        if (this.stationId != null) uri += "?station_id=" + this.stationId;
        this.serverURI = URI.create(uri);

        // Setup JSON parser
        this.jsonParser = new JSONParser();
    }

    @Override
    public void startClient()  {
        // Create HttpClient
        this.httpClient = HttpClient.newHttpClient();

        // Create a ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the task to run every 2 seconds
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending GET to " + this.serverURI + "...");
            HttpRequest request = this.createRequest(this.serverURI);
            HttpResponse<String> response = this.sendRequest(request);
            processResponse(response);
        }, 0, 2, TimeUnit.SECONDS);

        while (true) {}
    }

    @Override
    public void processCommand() {
        // Do nothing
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .headers(
                    "Content-Type", "text/plain",
                    "Lamport-Time", Integer.toString(this.lamportClock.getLamportTime())
            )
            .build();
    }

    @Override
    public HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processResponse(HttpResponse<String> response) {
        try {
            JSONObject jsonObject = (JSONObject) this.jsonParser.parse(response.body());
            jsonObject.keySet().forEach((Object key) -> {
                Object value = jsonObject.get(key);
                System.out.println(key + ": " + value);
            });
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Usage: java GETClient <hostname> <station_id>?");
            return;
        }

        // Construct a get client with the appropriate number of arguments
        String hostname = args[0];
        GETClient getClient;

        if (args.length > 2) {
            String station_id = args[2];
            getClient = new GETClient(hostname, station_id);
        } else {
            getClient = new GETClient(hostname);
        }

        // Start the client
        getClient.startClient();
    }
}
