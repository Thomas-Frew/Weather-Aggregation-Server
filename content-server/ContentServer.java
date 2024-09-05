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
    public void processResponse(HttpResponse<String> response) {
        System.out.println("Response received.");
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
