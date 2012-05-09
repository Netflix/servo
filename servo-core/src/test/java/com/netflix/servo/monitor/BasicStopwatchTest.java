package com.netflix.servo.monitor;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
/**
 * User: gorzell
 * Date: 5/1/12
 */
public class BasicStopwatchTest {
    private BasicStopwatch testStopwatch;

    @BeforeMethod
    public void setupTest() throws Exception {
        testStopwatch = new BasicStopwatch();
    }

    @Test
    public void testReset() throws Exception {
        testStopwatch.start();
        Thread.sleep(10);
        testStopwatch.stop();
        assertTrue(testStopwatch.getDuration() > 0);

        testStopwatch.reset();
        assertTrue(testStopwatch.getDuration() == -1);
    }

    @Test
    public void testGetDuration() throws Exception {
        testStopwatch.start();
        Thread.sleep(10);
        testStopwatch.stop();

        assertTrue(testStopwatch.getDuration() > 9000000);
    }

    @Test
    public void testGetDurationWithUnit() throws Exception {
        testStopwatch.start();
        Thread.sleep(10);
        testStopwatch.stop();

        assertTrue(testStopwatch.getDuration() > 12);
    }

    @Test
    public void testNotStarted() throws Exception {
        assertTrue(testStopwatch.getDuration() == -1);

        testStopwatch.stop();
        assertTrue(testStopwatch.getDuration() == -1);
    }

    @Test
    public void testNotStopped() throws Exception {
        assertTrue(testStopwatch.getDuration() == -1);

        testStopwatch.start();
        assertTrue(testStopwatch.getDuration() == -1);
    }
}
