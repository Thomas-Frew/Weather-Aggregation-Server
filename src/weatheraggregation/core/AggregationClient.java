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
    public LamportClock lamportClock;
    protected URI serverURI;
    protected HttpClient httpClient;
    private ScheduledExecutorService scheduler;

    // Attempts to send a message
    private int attempts = 0;

    // Message sending parameters
    public static final int MAX_RETRIES = 3;
    public static final int SLEEP_SECONDS = 2;

    /**
     * Start the client, sending requests on a regular schedule.
     * Requests are sent every SLEEP_SECONDS seconds.
     * If a request fails, we retry at most MAX_RETRIES times.
     */
    public final void startClient() {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending request to " + this.serverURI + "...");
            try {
                // Try to connect
                if (sendRequestWithRetry()) {
                    this.attempts = 0;
                } else {
                    this.attempts++;
                    if (attempts == MAX_RETRIES) throw new ConnectException("Could not connect to server.");
                }
            } catch (ConnectException e) {
                // If we failed to connect, shut down the client
                System.err.println("Could not connect to server.");
                this.shutdownClient();
            }
        }, 0, SLEEP_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Try to send a request. If this fails, print a message.
     * @return Whether the request was successfully sent.
     */
    public final boolean sendRequestWithRetry() {
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

    /**
     * Create an HTTP request. Implemented based on the clients' needs.
     * @return The created HTTP request.
     */
    public abstract HttpRequest createRequest();

    /**
     * Send an HTTP request.
     * @param request The HTTP request to send.
     * @return The response from the HTTP request.
     * @throws IOException The request can raise an IO exception.
     * @throws InterruptedException The request can be interrupted.
     */
    public final HttpResponse<String> sendRequest(HttpRequest request) throws IOException, InterruptedException {
        return this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Process an HTTP response. Implemented based on the clients' needs.
     * @param response The HTTP response to process.
     */
    public abstract void processResponse(HttpResponse<String> response);

    /**
     * Shut down the client immediately and print a message.
     */
    public void shutdownClient() {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.shutdownNow();
        }
        System.out.println("Client has been shut down.");
    }
}
