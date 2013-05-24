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

public class ResettableCounterTest extends AbstractMonitorTest<ResettableCounter> {

    public ResettableCounter newInstance(String name) {
        return new ResettableCounter(MonitorConfig.builder(name).build());
    }

    public ResettableCounter newInstance(String name, long interval) {
        return new ResettableCounter(MonitorConfig.builder(name).build(), interval);
    }

    @Test
    public void testHasGaugeTag() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "GAUGE");
    }

    @Test
    public void testGetValue() throws Exception {
        ResettableCounter c = newInstance("foo");
        assertEquals(c.getValue().longValue(), 0L);

        // Check basic rate, if sleep exactly 50ms rate since the reset would be 20.0
        c.increment();
        assertEquals(c.getCount(), 1L);
        Thread.sleep(50);
        double rate = c.getValue().doubleValue();
        assertTrue(rate <= 20.0 && rate > 0.0);

        // Should be around 10m, allow
        c.increment(1000000);
        assertEquals(c.getCount(), 1000001L);
        rate = c.getValue().doubleValue();
        assertTrue(rate <= 2.001e7 && rate > 5e6);
    }

    @Test
    public void testGetAndResetValue() throws Exception {
        ResettableCounter c = newInstance("foo");
        assertEquals(c.getValue().longValue(), 0L);

        // Check basic rate, if sleep exactly 50ms rate since the reset would be 20.0
        c.increment();
        Thread.sleep(50);
        double rate = c.getValue().doubleValue();
        assertTrue(rate <= 20.0 && rate > 0.0);

        // Should be around 10m, allow
        c.increment(1000000);
        assertEquals(c.getCount(), 1000001L);
        rate = c.getAndResetValue().doubleValue();
        assertEquals(c.getCount(), 0L);
        assertTrue(rate <= 2.001e7 && rate > 5e6);
        assertEquals(c.getValue().longValue(), 0L);
    }

    @Test
    public void testGetAndResetValueWithEstimate() throws Exception {
        ResettableCounter c = newInstance("foo", 60000L);
        assertEquals(c.getValue().longValue(), 0L);

        // Check basic rate, if sleep exactly 50ms rate since the reset would be 20.0
        c.increment();
        Thread.sleep(50);
        double rate = c.getValue().doubleValue();
        assertTrue(rate <= 0.2 && rate > 0.0);

        // Should be around 10m, allow
        c.increment(1000000);
        rate = c.getAndResetValue().doubleValue();
        assertTrue(rate <= 20.0e3 && rate > 10.0e3);
        assertEquals(c.getValue().longValue(), 0L);
    }

    @Test
    public void testOverflow() throws Exception {
        ResettableCounter c = newInstance("foo");
        assertEquals(c.getValue().longValue(), 0L);

        // Check basic rate, if sleep exactly 50ms rate since the reset would be 20.0
        c.increment(Long.MAX_VALUE);
        c.increment(1);
        long rate = c.getValue().longValue();
        assertEquals(rate, 0L);
    }
}
