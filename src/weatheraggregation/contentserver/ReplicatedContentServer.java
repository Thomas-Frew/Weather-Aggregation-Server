package weatheraggregation.contentserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReplicatedContentServer {
    private final List<ContentServer> contentServers;
    private int primaryIndex;
    public ScheduledExecutorService scheduler;

    public ReplicatedContentServer(List<String> serverHostnames, String contentFilename) {
        this.contentServers = new ArrayList<>();
        this.primaryIndex = 0;

        for (String hostname : serverHostnames) {
            contentServers.add(new ContentServer(hostname, contentFilename, this::promoteNextServer));
        }
    }

    public ContentServer getPrimaryServer() {
        return contentServers.get(primaryIndex);
    }

    public void promoteNextServer() {
        primaryIndex = (primaryIndex + 1) % contentServers.size();
        System.out.println("Promoted server " + primaryIndex + " to primary.");
        this.startPrimary();
    }

    public void startPrimary() {
        this.getPrimaryServer().startClient();
    }

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
