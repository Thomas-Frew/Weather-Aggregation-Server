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

    public final void startClient() {
        // Send request every 2 seconds
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending request to " + this.serverURI + "...");
            sendRequestWithRetry();
        }, 0, 2, TimeUnit.SECONDS);

        // Keep application alive
        try {
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Application interrupted: " + e.getMessage());
        }
    }

    public final void sendRequestWithRetry() {
        try {
            HttpRequest request = this.createRequest();
            HttpResponse<String> response = this.sendRequest(request);
            processResponse(response);
        } catch (IOException | InterruptedException e) {
            System.err.println("Request failed. Retrying...");
        }
    }

    public abstract HttpRequest createRequest();

    public final HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public abstract void processResponse(HttpResponse<String> response);
}
