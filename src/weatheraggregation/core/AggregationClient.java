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
    public Runnable shutdownCallback;

    public final void startClient() {
        // Send request every 2 seconds
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending request to " + this.serverURI + "...");
            try {
                sendRequestWithRetry();
            } catch (RuntimeException e) {
                this.shutdownClient();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    public final void sendRequestWithRetry() throws RuntimeException {
        try {
            HttpRequest request = this.createRequest();
            HttpResponse<String> response = this.sendRequest(request);
            processResponse(response);
        } catch (IOException | InterruptedException e) {
            System.err.println("Request failed. Retrying...");
            throw new RuntimeException();
        }
    }

    public abstract HttpRequest createRequest();

    public final HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public abstract void processResponse(HttpResponse<String> response);

    public void shutdownClient() {
        if (this.shutdownCallback != null) {
            this.shutdownCallback.run();
        }

        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdown();
            try {
                if (!this.scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.scheduler.shutdownNow();
            }
        }
        // Interrupt the main thread's sleep to allow shutdown
        Thread.currentThread().interrupt();
        System.out.println("Client has been shut down.");
    }
}
