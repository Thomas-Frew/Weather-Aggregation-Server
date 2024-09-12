import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ContentServer extends AggregationClient {
    public final String contentFilename;

    public ContentServer(String serverHostname, String contentFilename) {
        this.serverHostname = serverHostname;
        this.contentFilename = contentFilename;

        this.serverURI = URI.create("http://" + this.serverHostname);
        this.httpClient = HttpClient.newHttpClient();

        this.lamportClock = new LamportClockImpl();
    }

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
            throw new RuntimeException(e);
        }
    }

    @Override
    public void processResponse(HttpResponse<String> response) {
        int eventTime = Integer.parseInt(response.headers().firstValue("Lamport-time").orElse("0"));
        this.lamportClock.processEvent(eventTime);

        System.out.println("Received response with status code: " + response.statusCode());
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
