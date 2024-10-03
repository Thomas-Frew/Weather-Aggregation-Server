package weatheraggregation.replicatedcontentserver;

import weatheraggregation.contentserver.ContentServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A fault-tolerant ContentServer that contains several "backup" instances.
 * If ContentServer fails to publish to its AggregationServer, we fail over to the backups.
 */
public class ReplicatedContentServer {
    private final List<ContentServer> contentServers;
    private int primaryIndex;
    private boolean running = true;

    public ReplicatedContentServer(List<String> serverHostnames, String contentFilename) {
        this.contentServers = new ArrayList<>();
        this.primaryIndex = 0;

        for (String hostname : serverHostnames) {
            contentServers.add(new CallbackContentServer(hostname, contentFilename, this::promoteNextServer));
        }
    }

    /**
     * Get the primary ContentServer to send weather data from.
     * @return The primary ContentServer.
     */
    public ContentServer getPrimaryServer() {
        return contentServers.get(primaryIndex);
    }

    /**
     * Promote the next server to be a primary, then run it.
     */
    public void promoteNextServer() {
        // If we have shut down, ignore the promotion
        if (running) {
            primaryIndex = (primaryIndex + 1) % contentServers.size();
            System.out.println("Promoted server " + primaryIndex + " to primary.");
            this.startPrimary();
        }
    }

    /**
     * Start the primary ContentServer
     */
    public void startPrimary() {
        this.getPrimaryServer().startClient();
    }

    /**
     * Shut down the primary ContentServer, failing over.
     */
    public void shutdownPrimary() {
        this.getPrimaryServer().shutdownClient();
    }

    /**
     * Shut down the primary ContentServer and do not fail over, shutting off the client as a whole.
     */
    public void shutdownClient() {
        running = false;
        this.shutdownPrimary();
    }

    /**
     * The entry point for the client.
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java ReplicatedContentServer <content_filename> <hostname1> <hostname2> ...");
            return;
        }

        String contentFilename = args[0];
        List<String> serverHostnames = new ArrayList<>(Arrays.asList(args).subList(1, args.length));

        ReplicatedContentServer replicatedServer = new ReplicatedContentServer(serverHostnames, contentFilename);
        replicatedServer.startPrimary();
    }
}
