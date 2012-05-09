package com.netflix.servo.monitor;

import com.netflix.servo.MonitorContext;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * User: gorzell
 * Date: 5/1/12
 */
public class BasicTimerTest {
    private BasicTimer testTimer;

    @BeforeTest
    public void setup() throws Exception{
        testTimer = new BasicTimer(new MonitorContext.Builder("test").build(),TimeUnit.SECONDS);
    }

    @Test
    public void testGetTimeUnit() throws Exception {
        assertEquals(testTimer.getTimeUnit(), TimeUnit.SECONDS);
    }

    @Test
    public void testGetValue() throws Exception {
        testTimer.record(100);
        assertTrue(testTimer.getValue() == 100);

        testTimer.record(10000, TimeUnit.MILLISECONDS);
        assertTrue(testTimer.getValue() == 10);
    }
}
