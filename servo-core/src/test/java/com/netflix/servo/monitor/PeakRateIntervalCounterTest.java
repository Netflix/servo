/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.tag.Tag;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import org.testng.annotations.Test;

public class PeakRateIntervalCounterTest extends AbstractMonitorTest<PeakRateIntervalCounter> {

    @Override
    public PeakRateIntervalCounter newInstance(String name) {
        return new PeakRateIntervalCounter(MonitorConfig.builder(name).build(), 60);

    }

    
    @Test
    public void testIncrement() throws Exception {
        PeakRateIntervalCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L);


        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            c.increment();
        }

        peakCount = c.getValue();
        assertEquals(peakCount, 1L, "Delta of 5 in 5 seconds, e.g. peak rate = average, 1 per second");



        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            c.increment(3);
        }

        peakCount = c.getValue();
        assertEquals(peakCount, 3L, "Delta of 15 in 5 seconds, e.g. peak rate = average, 3 per second");


        Thread.sleep(1000L);
        c.increment(10);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(1000L);
            c.increment(3);
        }
        c.increment();

        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta of 15 in 5 seconds, e.g. peak rate = 10, average = 3, min = 1 per second");


        Thread.sleep(5000L);
        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta of 0 in 5 seconds, e.g. peak rate = previous max, 10 per second");


    }

    @Test
    public void testReset() throws Exception {
        PeakRateIntervalCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L);

        c.increment();
        peakCount = c.getValue();
        assertEquals(peakCount, 1L, "Delta 1 in first second");

        Thread.sleep(1000L);
        c.increment(5L);
        peakCount = c.getValue();
        assertEquals(peakCount, 5L, "Delta 5 in second second");

        Thread.sleep(2000L);
        c.increment(10L);
        peakCount = c.getAndResetValue();
        assertEquals(peakCount, 10L, "Delta 10 in fourth second before reset");

        peakCount = c.getValue();
        assertEquals(peakCount, 0, "After Reset");

        c.increment(8L);
        peakCount = c.getValue();
        assertEquals(peakCount, 8L, "Delta 8 in first second after reset");

    }

    @Test
    public void testHasGaugeTag() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "GAUGE");
    }

    @Test
    public void testEqualsCount() throws Exception {
        PeakRateIntervalCounter c1 = newInstance("foo");
        PeakRateIntervalCounter c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.increment();
        assertNotEquals(c1, c2);
        c2.increment();
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsAndHashCodeName() throws Exception {
        PeakRateIntervalCounter c1 = newInstance("1234567890");
        PeakRateIntervalCounter c2 = newInstance("1234567890");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        c2 = c1;
        assertEquals(c2, c1);
    }
}
