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

public class DoubleGaugeTest extends AbstractMonitorTest<DoubleGauge> {
    @Override
    public DoubleGauge newInstance(String name) {
        return new DoubleGauge(MonitorConfig.builder(name).build());
    }

    @Test
    public void testSet() throws Exception {
        DoubleGauge gauge = newInstance("test");
        gauge.set(10.0);
        assertEquals(gauge.getValue(), 10.0);
    }

    @Test
    public void testGetAndSet() throws Exception {
        DoubleGauge gauge = newInstance("test");
        gauge.set(10.0);
        double prevVal = gauge.getAndSet(20.0);
        assertEquals(prevVal, 10.0);
        assertEquals(gauge.getValue(), 20.0);
    }

    @Test
    public void testGetAndAdd() throws Exception {
        DoubleGauge gauge = newInstance("test");
        gauge.set(10.0);
        double prevVal = gauge.getAndAdd(2.0);
        assertEquals(prevVal, 10.0);
        assertEquals(gauge.getValue(), 12.0);
    }

    @Test
    public void testAddAndGet() throws Exception {
        DoubleGauge gauge = newInstance("test");
        gauge.set(10.0);
        double newVal = gauge.addAndGet(2.0);
        assertEquals(newVal, 12.0);
        assertEquals(gauge.getValue(), 12.0);
    }

    @Test
    public void testGetConfig() throws Exception {
        DoubleGauge gauge = newInstance("test");
        MonitorConfig expectedConfig = MonitorConfig.builder("test").withTag("type", "GAUGE").build();
        assertEquals(gauge.getConfig(), expectedConfig);
    }
}
