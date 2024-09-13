package weatheraggregation.test;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ContentServerTests {

    /*
    Try to send data to the server for the first time
     */
    @Test
    public void sendFirstData() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
Try to send data to the server for the first time
 */
    @Test
    public void sendRepeatedData() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Make the request again and get another response
        response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        responseStatus = response.statusCode();
        assertEquals(200, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(3, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }
}
