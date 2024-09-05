import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.*;

public class GETClient extends AggregationClient {
    public static final String CLIENT_NAME = "GET_CLIENT";
    public final String stationId;

    public GETClient(String serverHostname) {
        this(serverHostname, null);
    }

    public GETClient(String serverHostname, String stationId) {
        this.serverHostname = serverHostname;
        this.stationId = stationId;

        String uri = "http://" + this.serverHostname;
        if (this.stationId != null) uri += "?station_id=" + this.stationId;
        this.serverURI = URI.create(uri);
        this.httpClient = HttpClient.newHttpClient();

        this.lamportClock = new LamportClockImpl();
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .headers(
                    "User-Agent", "ATOMClient/1/0",
                    "Content-Type", "text/plain",
                    "Lamport-Time", Integer.toString(this.lamportClock.getLamportTime())
            )
            .build();
    }

    @Override
    public void processResponse(HttpResponse<String> response) {
        int otherTime = Integer.parseInt(response.headers().firstValue("Lamport-Time").orElse("0"));
        this.lamportClock.processEvent(otherTime);

        try {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(response.body());
            jsonObject.forEach((key, value) -> {
                System.out.println(key + ": " + value);
            });
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java GETClient <hostname> <station_id>?");
            return;
        }

        String hostname = args[0];
        GETClient getClient;

        if (args.length > 2) {
            String station_id = args[2];
            getClient = new GETClient(hostname, station_id);
        } else {
            getClient = new GETClient(hostname);
        }

        getClient.startClient();
    }
}
