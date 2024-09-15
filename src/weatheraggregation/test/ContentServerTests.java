package weatheraggregation.test;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;
import weatheraggregation.core.FileHelpers;
import weatheraggregation.getclient.GETClient;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ContentServerTests {

    /*
    Try to send data to the server for the first time
    */
    @Test
    public void sendFirstData() throws IOException, InterruptedException, ParseException {
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

        // Check if the file contents match what we expect
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to send data to the server for the first time
    */
    @Test
    public void sendRepeatedData() throws IOException, InterruptedException, ParseException {
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

        // Check if the file contents match what we expect
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Make the request again and get another response
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

    /*
    Try to send data to the server for the first time
    */
    @Test
    public void sendDifferentData() throws IOException, InterruptedException, ParseException {
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

        // Check if ID of the file matches what we expected
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00001");
        assertEquals(entries.getFirst()[2], "1");

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Create a new server and send a new request
        client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "content_data_2.tst");
        request = client.createRequest();
        response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 201
        responseStatus = response.statusCode();
        assertEquals(201, responseStatus);

        // Check if the file contents match what we expect
        entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS00002");
        assertEquals(entries.getFirst()[2], "2");
        assertEquals(entries.get(1)[0], "IDS00001");
        assertEquals(entries.get(1)[2], "1");

        // Test that the lamport time has been updated
        assertEquals(3, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to send with no valid fields
    */
    @Test
    public void sendDataWithoutValidFields() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "chat_content_data.tst");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(500, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Try to send data with some valid fields, but no ID
    */
    @Test
    public void sendDataWithoutID() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "no_id_content_data.tst");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(500, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
Try to send data with some valid fields, but no ID
*/
    @Test
    public void sendEmptyJSON() throws IOException, InterruptedException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "empty_content_data.tst");

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);
        client.processResponse(response);

        // Ensure the response is 200
        int responseStatus = response.statusCode();
        assertEquals(204, responseStatus);

        // Test that the lamport time has been updated
        assertEquals(2, client.lamportClock.getLamportTime());

        // Shutdown the server
        server.shutdownServer();
    }

    /*
    Integration Test: Test regular execution to see that data is pushed every 2 seconds.
     */
    @Test
    public void regularRequestsSent() throws IOException, InterruptedException, ParseException {
        // Set up the file, server and client
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "content_data_mixed.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, true);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "content_data_mixed.tst");

        // Make the request and get the response
        server.startServer();
        client.startClient();

        TimeUnit.SECONDS.sleep(3);

        // Test that the lamport time has been updated
        assertEquals(4, client.lamportClock.getLamportTime());

        // Check to ensure the data has been commited
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.getFirst()[0], "IDS12763");
        assertEquals(entries.getFirst()[2], "3");

        // Shutdown the server
        server.shutdownServer();
    }
}
