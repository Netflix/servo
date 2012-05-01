package com.netflix.servo.monitor;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This class does not enforce starting or stopping once and only once without a reset.
 */
public class BasicStopwatch implements Stopwatch {
    private AtomicLong startTime = new AtomicLong(-1);
    private AtomicLong endTime = new AtomicLong(-1);

    public BasicStopwatch() {
    }

    public BasicStopwatch(boolean started) {
        if (started) start();
    }

    /**
     * Start the stopwatch.
     */
    @Override
    public void start() {
        startTime.set(System.nanoTime());
    }

    /**
     * Stop the stopwatch.
     */
    @Override
    public void stop() {
        endTime.set(System.nanoTime());
    }

    /**
     * Reset the stopwatch so that it can be used again.
     */
    @Override
    public void reset() {
        startTime = new AtomicLong(-1);
        endTime = new AtomicLong(-1);
    }

    /**
     * Get the duration of time the stopwatch was running.
     *
     * @param timeUnit
     * @return duration in specified time unit.
     */
    @Override
    public long getDuration(TimeUnit timeUnit) {
        return timeUnit.convert(getDuration(), TimeUnit.NANOSECONDS);
    }

    /**
     * Get the duration in the default TimeUnit which is nano-seconds.
     *
     * @return
     */
    @Override
    public long getDuration() {
        return (endTime.get() < 0 || startTime.get() < 0) ? -1 : endTime.get() - startTime.get();
    }
}
