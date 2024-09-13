package weatheraggregation.test;

import weatheraggregation.core.LamportClock;
import weatheraggregation.core.LamportClockImpl;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LamportTests {

    @Test
    public void lamportClockStartsAtZero() {
        LamportClock clock = new LamportClockImpl();

        assertEquals(clock.getLamportTime(),0);
    }

    @Test
    public void lamportClockInternalIncrement() {
        LamportClock clock = new LamportClockImpl();

        clock.processEvent();
        assertEquals(clock.getLamportTime(),1);
        clock.processEvent();
        assertEquals(clock.getLamportTime(),2);
        clock.processEvent();
        assertEquals(clock.getLamportTime(),3);
    }

    @Test
    public void lamportClockLoadNewTime() {
        LamportClock clock = new LamportClockImpl();

        clock.processEvent(100);
        assertEquals(clock.getLamportTime(),101);
    }

    @Test
    public void lamportClockKeepOldTime() {
        LamportClock clock = new LamportClockImpl();

        clock.processEvent();
        assertEquals(clock.getLamportTime(),1);
        clock.processEvent();
        assertEquals(clock.getLamportTime(),2);
        clock.processEvent(0);
        assertEquals(clock.getLamportTime(),3);
    }
}