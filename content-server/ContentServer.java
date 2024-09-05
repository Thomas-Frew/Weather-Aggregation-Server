import org.json.simple.JSONObject;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ContentServer implements AggregationClient {

    LamportClock lamportClock;
    public final String serverHostname;
    public final String contentFilename;

    private final URI serverURI;
    private HttpClient httpClient;;

    public ContentServer(String serverHostname) {
        this(serverHostname, null);
    }

    public ContentServer(String serverHostname, String contentFilename) {
        this.lamportClock = new LamportClockImpl();
        this.serverHostname = serverHostname;
        this.contentFilename = contentFilename;

        // Establish permanent connection URI
        String uri = "http://" + this.serverHostname;
        this.serverURI = URI.create(uri);
    }

    @Override
    public void startClient()  {
        // Create HttpClient
        this.httpClient = HttpClient.newHttpClient();

        // Create a ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Schedule the task to run every 2 seconds
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("Sending PUT to " + this.serverURI + "...");
            HttpRequest request = this.createRequest(this.serverURI);
            HttpResponse<String> response = this.sendRequest(request);
            processResponse(response);
        }, 0, 2, TimeUnit.SECONDS);

        while (true) {}
    }

    @Override
    public void processCommand() {
        // Do nothing
    }

    private static Map<String, String> readFileToMap(String filePath) throws IOException {
        Map<String, String> jsonMap = new HashMap<>();
        String line;

        // Read each line of the file, splitting at colons
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":", 2);
                if (parts.length == 2) jsonMap.put(parts[0].trim(), parts[1].trim());

            }
        }
        return jsonMap;
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        try {
            // Convert file to JSON string
            Map<String, String> jsonPairs = readFileToMap(this.contentFilename);
            JSONObject jsonObject = new JSONObject(jsonPairs);
            String jsonString = jsonObject.toJSONString();

            // Create request
            return HttpRequest.newBuilder()
                    .uri(uri)
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonString))
                    .headers(
                            "User-Agent", "ATOMClient/1/0",
                            "Content-Type", "text/plain",
                            "Content-Length", Integer.toString(jsonString.length()),
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

        // Construct a content server with the appropriate number of arguments
        String hostname = args[0];
        String contentFilename = args[1];

        ContentServer contentServer = new ContentServer(hostname, contentFilename);
        contentServer.startClient();
    }
}
