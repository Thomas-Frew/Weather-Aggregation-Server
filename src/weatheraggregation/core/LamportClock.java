package weatheraggregation.core;

public interface LamportClock {
    void processEvent();
    void processEvent(int otherTime);
    int getLamportTime();
}
