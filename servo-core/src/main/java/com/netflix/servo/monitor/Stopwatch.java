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
     * Get the duration of time the stopwatch was running.
     * @param timeUnit
     * @return duration in specified time unit.
     */
    public long getDuration(TimeUnit timeUnit);
}
