package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;

public interface Stopwatch {

    /**
     * Creates a new Stopwatch that has not been started.
     * @return The new Stopwatch
     */
    public Stopwatch createStopwatch();

    /**
     * Creates a new Stopwatch instance.  allowing the caller to specify whether or not is started right away.
     * @param started
     * @return The new Stopwatch
     */
    public Stopwatch createStopwatch(boolean started);

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
    public double getDuration(TimeUnit timeUnit);
}
