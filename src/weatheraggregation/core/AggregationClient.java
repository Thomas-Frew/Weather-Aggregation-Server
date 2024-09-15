package weatheraggregation.core;

import java.io.IOException;
import java.net.ConnectException;
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
    public ScheduledExecutorService scheduler;

    public int attempts = 0;

    public static final int MAX_RETRIES = 3;
    public static final int SLEEP_SECONDS = 2;

    public final void startClient() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending request to " + this.serverURI + "...");
            try {
                if (sendRequestWithRetry()) {
                    attempts = 0;
                } else {
                    attempts++;
                    if (attempts == MAX_RETRIES) throw new ConnectException("Could not connect to server.");
                }
            } catch (ConnectException e) {
                System.err.println("Could not connect to server.");
                this.shutdownClient();
            }
        }, 0, SLEEP_SECONDS, TimeUnit.SECONDS);
    }

    public final boolean sendRequestWithRetry() throws ConnectException {
        try {
            HttpRequest request = this.createRequest();
            HttpResponse<String> response = this.sendRequest(request);
            processResponse(response);
            return true;
        } catch (IOException | InterruptedException e) {
            System.err.println("Request failed. Retrying...");
            return false;
        }
    }

    public abstract HttpRequest createRequest();

    public final HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public abstract void processResponse(HttpResponse<String> response);

    public void shutdownClient() {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdownNow();
        }
        System.out.println("Client has been shut down.");
    }
}
