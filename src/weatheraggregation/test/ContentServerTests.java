package weatheraggregation.test;

import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;
import weatheraggregation.core.AggregationClient;
import weatheraggregation.core.FileHelpers;
import weatheraggregation.jsonparser.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ContentServerTests {
    /**
     Send data to an AggregationServer for the first time, and confirm that it is committed.
     */
    @Test
    public void sendFirstData() throws IOException, InterruptedException, CustomParseException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Check that the file contents match what we expect
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Send data to an AggregationServer many times, and confirm that it is committed.
     */
    @Test
    public void sendRepeatedData() throws IOException, InterruptedException, CustomParseException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Check if the file contents match what we expect
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Make a request again and get another response
        request = client.createRequest();
        response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        responseStatus = response.statusCode();
        assertEquals(200, responseStatus);

        // Check if the file contents match what we expect
        entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "3");

        // Test that the lamport time has been updated
        assertEquals(4, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Send data from different weather stations and confirm that it is committed.
     */
    @Test
    public void sendDifferentData() throws IOException, InterruptedException, CustomParseException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Check if ID of the file matches what we expected
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Create a new server and send a new request
        client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_2.tst");
        request = client.createRequest();
        response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Check if the file contents match what we expect
        entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00002");
        assertEquals(entries.getFirst()[2], "1");
        assertEquals(entries.get(1)[0], "IDS00001");
        assertEquals(entries.get(1)[2], "1");

        // Test that the lamport time has been updated
        assertEquals(3, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Fail to send data that lacks any weather fields.
     */
    @Test
    public void sendDataWithoutValidFields() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/chat_content_data.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 500
        int responseStatus = response.statusCode();
        assertEquals(500, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Fail to send data that has some weather fields, but lacks an ID.
     */
    @Test
    public void sendDataWithoutID() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/no_id_content_data.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 500
        int responseStatus = response.statusCode();
        assertEquals(500, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Fail to send data that lacks any data.
     */
    @Test
    public void sendEmptyJSON() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/empty_content_data.tst");

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 204
        int responseStatus = response.statusCode();
        assertEquals(204, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Reject data that has an older Lamport clock time.
     */
    @Test
    public void sendOutdatedData() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client1 = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");
        ContentServer client2 = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Process lots of event with client1
        client1.lamportClock.processEvent(10);

        // Make a request and get a response
        server.startServer();
        HttpRequest request = client1.createRequest();
        HttpResponse<String> response = client1.sendRequest(request);
        client1.processResponse(response);

        // Ensure the response is 201
        int responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(13, client1.lamportClock.getLamportTime());

        // Make a request from the other client and get a response
        request = client2.createRequest();
        response = client2.sendRequest(request);
        client2.processResponse(response);

        // Ensure the response is 500
        responseStatus = response.statusCode();
        assertEquals(500, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(14, client2.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Integration Test: Push data every 2 seconds.
     */
    @Test
    public void regularRequestsSent() throws IOException, CustomParseException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_mixed.tst");

        // Make the request and get the response
        server.startServer();
        client.startClient();

        // Sleep for 1 cycle and 1 second (enough time for 2 requests to be made)
        TimeUnit.SECONDS.sleep(AggregationClient.SLEEP_SECONDS + 1);

        // Test that the lamport time has been updated
        assertEquals(4, client.lamportClock.getLamportTime());

        // Check to ensure the data has been commited
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS12763");
        assertEquals(entries.getFirst()[2], "3");

        // Shutdown the server
        client.shutdownClient();
        server.shutdownServer();
    }
}
