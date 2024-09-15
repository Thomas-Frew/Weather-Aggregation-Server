package weatheraggregation.contentserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

class CallbackContentServer extends ContentServer {
    private final Runnable shutdownCallback;

    public CallbackContentServer(String hostname, String contentFilename, Runnable shutdownCallback) {
        super(hostname, contentFilename);
        this.shutdownCallback = shutdownCallback;
    }

    public final void shutdownClient() {
        super.shutdownClient();
        if (shutdownCallback != null) shutdownCallback.run();
    }
}

public class ReplicatedContentServer {
    private final List<ContentServer> contentServers;
    private int primaryIndex;
    public ScheduledExecutorService scheduler;
    private boolean running = true;

    public ReplicatedContentServer(List<String> serverHostnames, String contentFilename) {
        this.contentServers = new ArrayList<>();
        this.primaryIndex = 0;

        for (String hostname : serverHostnames) {
            contentServers.add(new CallbackContentServer(hostname, contentFilename, this::promoteNextServer));
        }
    }

    public ContentServer getPrimaryServer() {
        return contentServers.get(primaryIndex);
    }

    public void promoteNextServer() {
        if (running) {
            primaryIndex = (primaryIndex + 1) % contentServers.size();
            System.out.println("Promoted server " + primaryIndex + " to primary.");
            this.startPrimary();
        }
    }

    public void startPrimary() {
        this.getPrimaryServer().startClient();
    }

    public void shutdownPrimary() {
        running = false;
        this.getPrimaryServer().shutdownClient();
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
