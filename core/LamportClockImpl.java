public class LamportClockImpl implements LamportClock {

    int lamportTime;

    public LamportClockImpl() {
        this.lamportTime = 0;
    }

    @Override
    public void processEvent() {
        this.lamportTime++;
    }

    @Override
    public void processEvent(int otherTime) {
        this.lamportTime = Math.max(this.lamportTime, otherTime) + 1;
    }

    @Override
    public int getLamportTime() {
        return this.lamportTime;
    }
}
