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

        this.serverURI = URI.create("http://" + this.serverHostname);
        this.httpClient = HttpClient.newHttpClient();

        this.lamportClock = new LamportClockImpl();
    }

    @Override
    public HttpRequest createRequest(URI uri) {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(uri);
        requestBuilder.GET();
        requestBuilder.headers(
            "User-agent", "ATOMClient/1/0",
            "Content-type", "text/plain",
            "Lamport-time", Integer.toString(this.lamportClock.getLamportTime())
        );

        if (this.stationId != null) {
            requestBuilder.header("Station-id", this.stationId);
        }

        return requestBuilder.build();
    }

    @Override
    public void processResponse(HttpResponse<String> response) {
        int responseStatus = response.statusCode();

        if (responseStatus == 200) {
            int eventTime = Integer.parseInt(response.headers().firstValue("Lamport-time").orElse("0"));
            this.lamportClock.processEvent(eventTime);

            try {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(response.body());
                jsonObject.forEach((key, value) -> {
                    System.out.println(key + ": " + value);
                });
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        } else if (responseStatus == 404) {
            System.err.println("Requested data not found.");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java GETClient <hostname> <station_id>?");
            return;
        }

        String hostname = args[0];
        GETClient getClient;

        if (args.length > 1) {
            String station_id = args[1];
            getClient = new GETClient(hostname, station_id);
        } else {
            getClient = new GETClient(hostname);
        }

        getClient.startClient();
    }
}
