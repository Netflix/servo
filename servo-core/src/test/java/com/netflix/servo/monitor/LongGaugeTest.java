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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class LongGaugeTest extends AbstractMonitorTest<LongGauge> {
    @Override
    public LongGauge newInstance(String name) {
        return new LongGauge(MonitorConfig.builder(name).build());
    }

    @Test
    public void testSet() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        assertEquals(gauge.getValue().longValue(), 10L);
    }

    @Test
    public void testGetValue() throws Exception {
        LongGauge gauge = newInstance("test");
        assertEquals(gauge.getValue().longValue(), 0L);
        gauge.set(10L);
        assertEquals(gauge.getValue().longValue(), 10L);
    }

    @Test
    public void testIncrementAndGet() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long newVal = gauge.incrementAndGet();
        assertEquals(newVal, 11L);
        assertEquals(gauge.getValue().longValue(), 11L);
    }

    @Test
    public void testAddAndGet() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long newVal = gauge.addAndGet(2L);
        assertEquals(newVal, 12L);
        assertEquals(gauge.getValue().longValue(), 12L);
    }

    @Test
    public void testDecrementAndGet() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long newVal = gauge.decrementAndGet();
        assertEquals(newVal, 9L);
        assertEquals(gauge.getValue().longValue(), 9L);
    }

    @Test
    public void testGetAndSet() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long prevVal = gauge.getAndSet(20L);
        assertEquals(prevVal, 10L);
        assertEquals(gauge.getValue().longValue(), 20L);
    }

    @Test
    public void testGetAndIncrement() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long prevVal = gauge.getAndIncrement();
        assertEquals(prevVal, 10L);
        assertEquals(gauge.getValue().longValue(), 11L);
    }

    @Test
    public void testGetAndAdd() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long prevVal = gauge.getAndAdd(2L);
        assertEquals(prevVal, 10L);
        assertEquals(gauge.getValue().longValue(), 12L);
    }

    @Test
    public void testGetAndDecrement() throws Exception {
        LongGauge gauge = newInstance("test");
        gauge.set(10L);
        long prevVal = gauge.getAndDecrement();
        assertEquals(prevVal, 10L);
        assertEquals(gauge.getValue().longValue(), 9L);
    }

    @Test
    public void testGetConfig() throws Exception {
        LongGauge gauge = newInstance("test");
        MonitorConfig expectedConfig = MonitorConfig.builder("test").withTag("type", "GAUGE").build();
        assertEquals(gauge.getConfig(), expectedConfig);
    }

}
