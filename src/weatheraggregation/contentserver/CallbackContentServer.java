package weatheraggregation.contentserver;

public class CallbackContentServer extends ContentServer {
    private final Runnable shutdownCallback;

    public CallbackContentServer(String hostname, String contentFilename, Runnable shutdownCallback) {
        super(hostname, contentFilename);
        this.shutdownCallback = shutdownCallback;
    }

    public final void shutdownClient() {
        super.shutdownClient();
        if (shutdownCallback != null) {
            shutdownCallback.run();
        }
    }
}
