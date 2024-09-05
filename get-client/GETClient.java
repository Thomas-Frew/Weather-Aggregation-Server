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
    // Constants
    public static final String CLIENT_NAME = "GET_CLIENT";

    // Command-line arguments
    public final String serverHostname;
    public final String stationId;

    // Networking
    private final URI serverURI;
    private final HttpClient httpClient;

    // Synchrony
    LamportClock lamportClock;

    public GETClient(String serverHostname) {
        this(serverHostname, null);
    }

    public GETClient(String serverHostname, String stationId) {
        this.serverHostname = serverHostname;
        this.stationId = stationId;

        String uri = "http://" + this.serverHostname;
        if (this.stationId != null) uri += "?station_id=" + this.stationId;
        this.serverURI = URI.create(uri);
        this.httpClient = HttpClient.newHttpClient();

        this.lamportClock = new LamportClockImpl();
    }

    @Override
    public void startClient()  {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending GET to " + this.serverURI + "...");
            sendRequest();
        }, 0, 2, TimeUnit.SECONDS);

        // Keep application alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Application interrupted: " + e.getMessage());
        }
    }

    public void sendRequest() {
        HttpRequest request = this.createRequest(this.serverURI);
        HttpResponse<String> response = this.sendRequest(request);
        processResponse(response);
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .headers(
                    "User-Agent", "ATOMClient/1/0",
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
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response.body());
            jsonObject.forEach((key, value) -> {
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

        String hostname = args[0];
        GETClient getClient;

        if (args.length > 2) {
            String station_id = args[2];
            getClient = new GETClient(hostname, station_id);
        } else {
            getClient = new GETClient(hostname);
        }

        getClient.startClient();
    }
}
