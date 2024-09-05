import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentServer implements AggregationClient {
    // Command-line arguments
    public final String serverHostname;
    public final String contentFilename;

    // Networking
    private final URI serverURI;
    private final HttpClient httpClient;

    // Synchrony
    LamportClock lamportClock;

    public ContentServer(String serverHostname, String contentFilename) {
        this.serverHostname = serverHostname;
        this.contentFilename = contentFilename;

        this.serverURI = URI.create("http://" + this.serverHostname);
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
        try {
            // Convert file to JSON string
            String jsonString = ConversionHelpers.readContentFile(this.contentFilename);

            // Create request
            return HttpRequest.newBuilder()
                    .uri(uri)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonString))
                    .headers(
                            "User-Agent", "ATOMClient/1/0",
                            "Content-Type", "text/plain",
                            "Lamport-Time", Integer.toString(this.lamportClock.getLamportTime())
                    )
                    .build();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        System.out.println(response.body());
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java ContentServer <hostname> <content_filename>");
            return;
        }

        String hostname = args[0];
        String contentFilename = args[1];

        ContentServer contentServer = new ContentServer(hostname, contentFilename);
        contentServer.startClient();
    }
}
