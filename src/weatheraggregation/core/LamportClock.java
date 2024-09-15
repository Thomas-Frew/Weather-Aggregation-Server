package weatheraggregation.core;

public interface LamportClock {
    /**
     * Process an internal event.
     */
    void processEvent();

    /**
     * Process an external event
     * @param otherTime The lamport time from the external event.
     */
    void processEvent(int otherTime);

    /**
     * Return the current lamport time.
     * @return The current lamport time.
     */
    int getLamportTime();
}
