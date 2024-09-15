package weatheraggregation.getclient;

import weatheraggregation.core.AggregationClient;
import weatheraggregation.core.LamportClockImpl;
import weatheraggregation.jsonparser.CustomJsonParser;
import weatheraggregation.jsonparser.CustomParseException;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class GETClient extends AggregationClient {
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
    public HttpRequest createRequest() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.uri(this.serverURI);
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
        int eventTime = Integer.parseInt(response.headers().firstValue("Lamport-time").orElse("0"));
        this.lamportClock.processEvent(eventTime);

        int responseStatus = response.statusCode();
        if (responseStatus == 200) {
            try {
                Map<String, String> jsonObject = CustomJsonParser.stringToJson(response.body());
                jsonObject.forEach((key, value) -> System.out.println(key + ": " + value));
            } catch (CustomParseException e) {
                throw new RuntimeException(e);
            }
        } else if (responseStatus == 404) {
            System.err.println("Requested data not found.");
        }
    }

    public static void main(String[] args)  {
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
