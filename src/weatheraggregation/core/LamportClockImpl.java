package weatheraggregation.core;

public class LamportClockImpl implements LamportClock {

    int lamportTime;

    public LamportClockImpl() {
        this.lamportTime = 0;
    }

    /**
     * Process an internal event.
     */
    @Override
    public void processEvent() {
        this.lamportTime++;
    }

    /**
     * Process an external event
     * @param otherTime The lamport time from the external event.
     */
    @Override
    public void processEvent(int otherTime) {
        this.lamportTime = Math.max(this.lamportTime, otherTime) + 1;
    }

    /**
     * Return the current lamport time.
     * @return The current lamport time.
     */
    @Override
    public int getLamportTime() {
        return this.lamportTime;
    }
}
