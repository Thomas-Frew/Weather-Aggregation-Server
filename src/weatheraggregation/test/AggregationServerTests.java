package weatheraggregation.test;

import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;
import weatheraggregation.core.FileHelpers;
import weatheraggregation.jsonparser.*;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class AggregationServerTests {
    /**
     Purge all outdated data on startup.
     */
    @Test
    public void expungeDataOnStartup() throws IOException, InterruptedException, CustomParseException {
        // Set up the file and server
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, false);

        // Start the server
        server.startServer();

        // Wait a second for the expunging to complete
        TimeUnit.SECONDS.sleep(1);

        // Ensure that the outdated data is expunged
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(entries.size(), 0);

        // Shutdown the server
        server.shutdownServer();
    }

    /**
     Purge outdated data every 30 seconds. Remove data that is 30 or more seconds old.
     */
    @Test
    public void expungeDataRegularly() throws IOException, InterruptedException, CustomParseException {
        // Set up the aggregationServer (server) and contentServer (client)
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, TestHelpers.PORT, false);
        ContentServer client = new ContentServer(TestHelpers.HOSTNAME, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Make a request
        server.startServer();
        HttpRequest request = client.createRequest();
        client.sendRequest(request);

        // Ensure that the outdated data was received
        List<String[]> entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(1, entries.size());

        // Wait 15 seconds
        TimeUnit.SECONDS.sleep(15);

        // Ensure that the outdated data has not been expunged
        entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(1, entries.size());

        // Wait 20 seconds
        TimeUnit.SECONDS.sleep(30);

        // Ensure that the outdated data has been expunged
        entries = FileHelpers.readWeatherFileAll(TestHelpers.WEATHER_DATA_FILENAME);
        assertEquals(0, entries.size());

        // Shutdown the server
        server.shutdownServer();
    }
}
