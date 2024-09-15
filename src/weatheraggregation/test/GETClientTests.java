package weatheraggregation.test;

import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.core.AggregationClient;
import weatheraggregation.jsonparser.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import weatheraggregation.getclient.GETClient;
import weatheraggregation.jsonparser.CustomJsonParser;

public class GETClientTests {
    /*
    Try to fetch the only data entry from a server.
     */
    @Test
    public void fetchSoleData() throws IOException, InterruptedException, CustomParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME);

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(200, responseStatus);

        // Test headers for correctness
        System.out.println(response.body());
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(response.body());
        System.out.println(jsonObject);
        assertEquals("CityTest1", jsonObject.get("name"));
        assertEquals("TEST00001", jsonObject.get("id"));

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to fetch the most recent data entry from a server.
     */
    @Test
    public void fetchMostRecentData() throws IOException, InterruptedException, CustomParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/3_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME);

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(200, responseStatus);

        // Test headers for correctness
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(response.body());
        assertEquals("CityTest3", jsonObject.get("name"));
        assertEquals("TEST00003", jsonObject.get("id"));

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to fetch a specific data entry from a server.
     */
    @Test
    public void fetchSpecificData() throws IOException, InterruptedException, CustomParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/3_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME, "TEST00002");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(200, responseStatus);

        // Test headers for correctness
        Map<String, String> jsonObject = CustomJsonParser.stringToJson(response.body());
        assertEquals("CityTest2", jsonObject.get("name"));
        assertEquals("TEST00002", jsonObject.get("id"));

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to fetch a non-existent data entry from a server.
     */
    @Test
    public void fetchMissingData() throws IOException, InterruptedException, CustomParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/3_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME, "MISSING");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(404, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to fetch data from a server that has no contents.
     */
    @Test
    public void fetchNoData() throws IOException, InterruptedException, CustomParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME);

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(404, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Integration Test: Test regular execution to see that data is fetched every 2 seconds.
     */
    @Test
    public void regularRequestsSent() throws IOException, InterruptedException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        GETClient client = new GETClient(TestHelpers.HOSTNAME);

        // Make the request and get the response
        server.startServer();
        client.startClient();

        // Sleep for 1 cycle and 1 second (enough time for 2 requests to be made)
        TimeUnit.SECONDS.sleep(AggregationClient.SLEEP_SECONDS + 1);

        // Test that the lamport time has been updated
        assertEquals(4, client.lamportClock.getLamportTime());

        // Shutdown the server
        client.shutdownClient();
        server.shutdownServer();
    }
}
