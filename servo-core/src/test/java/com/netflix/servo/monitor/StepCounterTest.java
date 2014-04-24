/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class StepCounterTest {

    final ManualClock clock = new ManualClock(50 * Pollers.POLLING_INTERVALS[1]);

    public StepCounter newInstance(String name) {
        return new StepCounter(MonitorConfig.builder(name).build(), clock);
    }

    public long time(long t) {
        return t * 1000 + Pollers.POLLING_INTERVALS[1] * 50;
    }

    @Test
    public void testSimpleTransition() {
        clock.set(0);
        StepCounter c = newInstance("c");
        assertEquals(c.getValue(0).doubleValue(), Double.NaN);
        assertEquals(c.getCurrentCount(0), 0L);

        clock.set(2000);
        c.increment();
        assertEquals(c.getValue(0).doubleValue(), Double.NaN);
        assertEquals(c.getCurrentCount(0), 1L);

        clock.set(52000);
        c.increment();
        assertEquals(c.getValue(0).doubleValue(), Double.NaN);
        assertEquals(c.getCurrentCount(0), 2L);

        clock.set(62000);
        c.increment();
        assertEquals(c.getValue(0).doubleValue(), 1.0 / 30.0);
        assertEquals(c.getCurrentCount(0), 1L);
    }


    @Test
    public void testInitialPollIsZero() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        assertEquals(c.getValue(1).doubleValue(), 0.0);
    }

    @Test
    public void testHasRightType() throws Exception {
        assertEquals(newInstance("foo").getConfig().getTags().getValue("type"), "GAUGE");
    }

    @Test
    public void testBoundaryTransition() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");

        // Should all go to one bucket
        c.increment();
        clock.set(time(4));
        c.increment();
        clock.set(time(9));
        c.increment();

        // Should cause transition
        clock.set(time(10));
        c.increment();
        clock.set(time(19));
        c.increment();

        // Check counts
        assertEquals(c.getValue(1).doubleValue(), 0.3);
        assertEquals(c.getCurrentCount(1), 2);
    }

    @Test
    public void testResetPreviousValue() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        for (int i = 1; i <= 100000; ++i) {
            c.increment();
            clock.set(time(i * 10 + 1));
            assertEquals(c.getValue(1).doubleValue(), 0.1);
        }
    }

    @Test
    public void testMissedInterval() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        c.getValue(1);

        // Multiple updates without polling
        c.increment();
        clock.set(time(4));
        c.increment();
        clock.set(time(14));
        c.increment();
        clock.set(time(24));
        c.increment();
        clock.set(time(34));
        c.increment();

        // Check counts
        assertTrue(Double.isNaN(c.getValue(1).doubleValue()));
        assertEquals(c.getCurrentCount(1), 1);
    }

    @Test
    public void testNonMonotonicClock() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        c.getValue(1);

        c.increment();
        c.increment();
        clock.set(time(10));
        c.increment();
        clock.set(time(9)); // Should get ignored
        c.increment();
        assertEquals(c.getCurrentCount(1), 2);
        c.increment();
        clock.set(time(10));
        c.increment();
        c.increment();
        assertEquals(c.getCurrentCount(1), 5);

        // Check rate for previous internval
        assertEquals(c.getValue(1).doubleValue(), 0.2);
    }
}
