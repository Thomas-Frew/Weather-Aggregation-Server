package weatheraggregation.contentserver;

import weatheraggregation.core.AggregationClient;
import weatheraggregation.core.FileHelpers;
import weatheraggregation.core.LamportClockImpl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A client that can publish weather data to an AggregationServer.
 * Weather data is published on a regular schedule.
 */
public class ContentServer extends AggregationClient {
    // The filename containing the content data to copy
    public final String contentFilename;

    public ContentServer(String serverHostname, String contentFilename) {
        this.serverHostname = serverHostname;
        this.contentFilename = contentFilename;

        this.serverURI = URI.create("http://" + this.serverHostname);
        this.httpClient = HttpClient.newHttpClient();

        this.lamportClock = new LamportClockImpl();
    }

    /**
     * Create an HTTP request to PUT weather data on the server.
     * @return The created HTTP request.
     */
    @Override
    public HttpRequest createRequest() {
        try {
            // Convert file to JSON string
            String jsonString = FileHelpers.readContentFile(this.contentFilename);

            // Create request
            return HttpRequest.newBuilder()
                    .uri(this.serverURI)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonString))
                    .headers(
                            "User-agent", "ATOMClient/1/0",
                            "Content-type", "text/plain",
                            "Lamport-time", Integer.toString(this.lamportClock.getLamportTime())
                    )
                    .build();

        } catch (IOException e) {
            System.err.println("Failed to create PUT request: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Process an HTTP response to PUT weather data on the server.
     * @param response The HTTP response to process.
     */
    @Override
    public void processResponse(HttpResponse<String> response) {
        int eventTime = Integer.parseInt(response.headers().firstValue("Lamport-time").orElse("0"));
        this.lamportClock.processEvent(eventTime);

        System.out.println("Received response with status code: " + response.statusCode());
    }

    /**
     * The entry point for the client.
     * @param args Command-line arguments.
     */
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
