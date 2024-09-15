package weatheraggregation.test;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.contentserver.ContentServer;
import weatheraggregation.contentserver.ReplicatedContentServer;
import weatheraggregation.core.AggregationClient;
import weatheraggregation.core.FileHelpers;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ReplicatedContentServerTests {

    private static final int PORT_1 = 2764;
    private static final int PORT_2 = 2765;
    private static final int PORT_3 = 2766;

    private static final String REPLICA_HOSTNAME_1 = TestHelpers.IP + ":" + PORT_1;
    private static final String REPLICA_HOSTNAME_2 = TestHelpers.IP + ":" + PORT_2;
    private static final String REPLICA_HOSTNAME_3 = TestHelpers.IP + ":" + PORT_3;

    private static final int FAILOVER_TIME = AggregationClient.MAX_RETRIES*(AggregationClient.SLEEP_SECONDS*2) + 2;

    /*
    Create a replicated content server with one client and confirm that it is the primary.
    */
    @Test
    public void initialConfigurationValid() {
        // Set up the replicatedContentServer with two hostnames
        List<String> hostnames = List.of(REPLICA_HOSTNAME_1, REPLICA_HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Check that the first hostname is the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_1);
    }

    /*
    Create a replicated content server with two valid clients and confirm that failover is not necessary.
    */
    @Test
    public void noFailover() throws IOException, InterruptedException {
        // Set up the aggregationServers
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server1 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_1, true);
        AggregationServer server2 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_2, true);

        // Set up the replicatedContentServer with two hostnames
        List<String> hostnames = List.of(REPLICA_HOSTNAME_1, REPLICA_HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Start up the aggregationServers
        server1.startServer();
        server2.startServer();

        // Start up the replicatedContentServer and wait 1.5 cycles
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(AggregationClient.SLEEP_SECONDS + 1);

        // Check that the first hostname is still the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_1);

        // Shut down the aggregationServers
        replicatedClient.shutdownPrimary();
        server1.shutdownServer();
        server2.shutdownServer();
    }

    /*
    Create a replicated content server with two valid clients and confirm that failover is not necessary.
    */
    @Test
    public void failover() throws IOException, InterruptedException {
        // Set up the aggregationServer
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server2 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_2, true);

        // Set up the replicatedContentServer with two hostnames
        List<String> hostnames = List.of(REPLICA_HOSTNAME_1, REPLICA_HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Start up the aggregationServer
        server2.startServer();

        // Start up the replicatedContentServer and wait 1.5 cycles
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the second hostname is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_2);

        // Shut down the aggregationServer
        replicatedClient.shutdownPrimary();
        server2.shutdownServer();
    }

    /*
    Create a replicated content server with two valid clients and confirm that failover is not necessary.
    */
    @Test
    public void doubleFailover() throws IOException, InterruptedException {
        // Set up the aggregationServer
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server3 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_3, true);

        // Set up the replicatedContentServer with two hostnames
        List<String> hostnames = List.of(REPLICA_HOSTNAME_1, REPLICA_HOSTNAME_2, REPLICA_HOSTNAME_3);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Start up the aggregationServer
        server3.startServer();

        // Start up the replicatedContentServer and wait to fail over twice
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the third hostname is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_3);

        // Shut down the aggregationServer
        replicatedClient.shutdownPrimary();
        server3.shutdownServer();
    }

    /*
    Create a replicated content server with two invalid clients and confirm that they cycle correctly.
    */
    @Test
    public void failBack() throws IOException, InterruptedException {
        // Set up the aggregationServer
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/1_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);

        // Set up the replicatedContentServer with two hostnames
        List<String> hostnames = List.of(REPLICA_HOSTNAME_1, REPLICA_HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "content_data_1.tst");

        // Start up the replicatedContentServer and wait to fail over
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the second hostname is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_2);

        // Wait to fail over
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the first hostname is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, REPLICA_HOSTNAME_1);

        // Shut down the aggregationServer
        replicatedClient.shutdownPrimary();
    }
}