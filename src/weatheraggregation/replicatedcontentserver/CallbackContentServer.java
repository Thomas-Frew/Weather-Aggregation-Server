package weatheraggregation.replicatedcontentserver;

import weatheraggregation.contentserver.ContentServer;

/**
 * A wrapper for a ContentServer that calls a callback when it dies.
 */
class CallbackContentServer extends ContentServer {
    // The callback to execute when the ContentServer shuts down
    private final Runnable shutdownCallback;

    public CallbackContentServer(String hostname, String contentFilename, Runnable shutdownCallback) {
        super(hostname, contentFilename);
        this.shutdownCallback = shutdownCallback;
    }

    /**
     * Shuts down the ContentServer, then calls the callback.
     */
    public final void shutdownClient() {
        super.shutdownClient();
        if (shutdownCallback != null) shutdownCallback.run();
    }
}
