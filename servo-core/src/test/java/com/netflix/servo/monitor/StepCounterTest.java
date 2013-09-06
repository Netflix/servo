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

import com.netflix.servo.tag.Tag;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StepCounterTest {

    static final long STEP = 10000L;

    final ManualClock clock = new ManualClock(STEP * 50);

    public StepCounter newInstance(String name) {
        return new StepCounter(MonitorConfig.builder(name).build(), STEP, clock);
    }

    public long time(long t) {
        return t * 1000 + STEP * 50;
    }

    @Test
    public void testInitialPollIsZero() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        assertEquals(c.getValue(), 0.0);
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
        assertEquals(c.getValue(), 0.3);
        assertEquals(c.getCurrentCount(), 2);
    }

    @Test
    public void testResetPreviousValue() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        for (int i = 1; i <= 1000000; ++i) {
            c.increment();
            clock.set(time(i * 10 + 1));
            assertEquals(c.getValue(), 0.1);
        }
    }

    @Test
    public void testMissedInterval() {
        clock.set(time(1));
        StepCounter c = newInstance("foo");
        c.getValue();

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
        assertTrue(Double.isNaN(c.getValue().doubleValue()));
        assertEquals(c.getCurrentCount(), 1);
    }
}
