package weatheraggregation.test;

import org.junit.Test;
import weatheraggregation.aggregationserver.AggregationServer;
import weatheraggregation.replicatedcontentserver.ReplicatedContentServer;
import weatheraggregation.core.AggregationClient;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class ReplicatedContentServerTests {

    // Example port numbers for the three AggregationServers we might want to connect to
    private static final int PORT_1 = 2764;
    private static final int PORT_2 = 2765;
    private static final int PORT_3 = 2766;

    // Construct hostnames for our three AggregationServers
    private static final String HOSTNAME_1 = TestHelpers.IP + ":" + PORT_1;
    private static final String HOSTNAME_2 = TestHelpers.IP + ":" + PORT_2;
    private static final String HOSTNAME_3 = TestHelpers.IP + ":" + PORT_3;

    // A constant (approximate) time required to fail over, this depends on the socket
    private static final int FAILOVER_TIME = AggregationClient.MAX_RETRIES*(AggregationClient.SLEEP_SECONDS*2) + 2;

    /**
     Create a ReplicatedContentServer with one ContentServer and confirm that it is the primary.
    */
    @Test
    public void initialConfigurationValid() {
        // Set up the ReplicatedContentServer with two ContentServers
        List<String> hostnames = List.of(HOSTNAME_1, HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Check that the first ContentServer is the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_1);
    }

    /**
     Create a ReplicatedContentServer with two ContentServers.
     Both of the ContentServers have AggregationServers to connect to, so no failover should occur.
     */
    @Test
    public void noFailover() throws IOException, InterruptedException {
        // Set up the two AggregationServers and its file
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server1 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_1, true);
        AggregationServer server2 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_2, true);

        // Set up the ReplicatedContentServer with two ContentServers
        List<String> hostnames = List.of(HOSTNAME_1, HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Start the AggregationServers
        server1.startServer();
        server2.startServer();

        // Start the ReplicatedContentServer and wait for 1.5 cycles
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(AggregationClient.SLEEP_SECONDS + 1);

        // Check that the first ContentServer is still the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_1);

        // Shut down all servers and clients
        replicatedClient.shutdownClient();
        server1.shutdownServer();
        server2.shutdownServer();
    }

    /**
     Create a ReplicatedContentServer with two ContentServers.
     The first ContentServer is missing its AggregationServer, causing failover.
     */
    @Test
    public void failover() throws IOException, InterruptedException {
        // Set up the AggregationServer and its file
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server2 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_2, true);

        // Set up the ReplicatedContentServer with two ContentServers
        List<String> hostnames = List.of(HOSTNAME_1, HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Start up the AggregationServer
        server2.startServer();

        // Start up the ReplicatedContentServer and wait for failover
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the second ContentServer is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_2);

        // Shut down all servers and clients
        replicatedClient.shutdownClient();
        server2.shutdownServer();
    }

    /**
     Create a ReplicatedContentServer with three ContentServers.
     The first two ContentServers are missing its AggregationServers, causing two failovers.
     */
    @Test
    public void doubleFailover() throws IOException, InterruptedException {
        // Set up the AggregationServer and its file
        TestHelpers.swapFiles(TestHelpers.DIRECTORY + "testdata/0_entry.tst", TestHelpers.WEATHER_DATA_FILENAME);
        AggregationServer server3 = new AggregationServer(TestHelpers.WEATHER_DATA_FILENAME, PORT_3, true);

        // Set up the ReplicatedContentServer with three ContentServers
        List<String> hostnames = List.of(HOSTNAME_1, HOSTNAME_2, HOSTNAME_3);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Start up the AggregationServer
        server3.startServer();

        // Start up the ReplicatedContentServer and wait to failover twice
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the third ContentServer is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_3);

        // Shut down all servers and clients
        replicatedClient.shutdownClient();
        server3.shutdownServer();
    }

    /**
     Create a ReplicatedContentServer with two ContentServers.
     All ContentServers are missing its AggregationServers, causing continued failover between the two.
     */
    @Test
    public void failBack() throws InterruptedException {
        // Set up the ReplicatedContentServer with two ContentServers
        List<String> hostnames = List.of(HOSTNAME_1, HOSTNAME_2);
        ReplicatedContentServer replicatedClient = new ReplicatedContentServer(hostnames, TestHelpers.DIRECTORY + "testdata/content_data_1.tst");

        // Start up the ReplicatedContentServer and wait to failover
        replicatedClient.startPrimary();
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the second ContentServer is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_2);

        // Wait to failover again
        TimeUnit.SECONDS.sleep(FAILOVER_TIME);

        // Check that the first ContentServer is now the primary
        assertEquals(replicatedClient.getPrimaryServer().serverHostname, HOSTNAME_1);

        // Shut down the client
        replicatedClient.shutdownClient();
    }
}