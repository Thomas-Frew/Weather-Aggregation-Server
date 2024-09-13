/*
import org.junit.Test;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.Assert.assertEquals;

public class GETClientTests {

    private static final int TEST_PORT = 4567;
    private static final String TEST_IP = "192.168.56.1";
    private static final String TEST_HOSTNAME = TEST_IP+":"+TEST_PORT;
    private static final String TEST_WEATHER_DATA_FILENAME = "test_weather_data.txt";

    @Test
    public void fetchAnyData() throws IOException, InterruptedException {

        // Set up the file, server and client
        TestHelpers.swapFiles("integration-tests/single_entry_1.txt", TEST_WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TEST_WEATHER_DATA_FILENAME, TEST_PORT);
        GETClient client = new GETClient(TEST_HOSTNAME);

        // Make the request and get the response
        server.startServer();
        HttpRequest request = client.createRequest();
        HttpResponse<String> response = client.sendRequest(request);

        int responseStatus = response.statusCode();
        assertEquals(200, responseStatus);
    }
}
*/