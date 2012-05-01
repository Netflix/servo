package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;

public interface Stopwatch {

    /**
     * Start the stopwatch.
     */
    public void start();

    /**
     * Stop the stopwatch.
     */
    public void stop();

    /**
     * Reset the stopwatch so that it can be used again.
     */
    public void reset();

    /**
     * Get the duration of time the stopwatch was running.
     * @param timeUnit
     * @return duration in specified time unit.
     */
    public long getDuration(TimeUnit timeUnit);

    /**
     * Get the duration in the default TimeUnit which is nano-seconds.
     * @return
     */
    public long getDuration();
}
