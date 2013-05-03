package com.netflix.servo.monitor;

/**
 * Date: 5/3/13
 * Time: 11:36 AM
 *
 * @author gorzell
 *         Stopwatch that will also record to a timer.
 */
public class TimedStopwatch extends BasicStopwatch {
    private final Timer timer;

    public TimedStopwatch(Timer timer) {
        this.timer = timer;
    }

    @Override
    public void stop() {
        super.stop();
        if (timer != null) {
            timer.record(getDuration(timer.getTimeUnit()));
        }
    }
}
