package converter;

import java.io.Serializable;

public class TimeInMillis implements Serializable {

    private long time;

    TimeInMillis() {
        this.time = System.currentTimeMillis();
    }

    public boolean hasMillisPast(long millis) {
        return (System.currentTimeMillis() >= (time + millis));
    }

    public boolean hasSecondsPast(long seconds) {
        return hasMillisPast(seconds * 1000);
    }

    public boolean hasMinutesPast(long minutes) {
        return hasSecondsPast(minutes * 60);
    }

    public void waitForMillisPast(long millis) {
        if (!hasMillisPast(millis)) {
            long timeToSleep = millis + this.time - System.currentTimeMillis();
            if (timeToSleep > 0) {
                sleepFor(timeToSleep);
            }
        }
    }

    public void waitForSecondsPast(long seconds) {
        waitForMillisPast(seconds * 1000);
    }

    public void waitForMinutesPast(long minutes) {
        waitForSecondsPast(minutes * 60);
    }

    private void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
