import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class AggregationClient {
    public String serverHostname;
    public URI serverURI;
    public HttpClient httpClient;
    public LamportClock lamportClock;

    public final void startClient()  {
        // Send request every 2 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending request to " + this.serverURI + "...");
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

    public final void sendRequest() {
        HttpRequest request = this.createRequest(this.serverURI);
        HttpResponse<String> response = this.sendRequest(request);
        processResponse(response);
    }

    public abstract HttpRequest createRequest(URI uri);

    public final HttpResponse<String> sendRequest(HttpRequest request) {
        try {
            return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void processResponse(HttpResponse<String> response);
}
