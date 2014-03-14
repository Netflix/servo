/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.publish;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.Metric;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.util.ManualClock;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class NormalizationTransformTest {
    Metric newMetric(long t, double v) {
        return new Metric(MonitorConfig.builder("test").build(), t, v);
    }

    static class TimeVal {
        final long t;
        final double v;

        static TimeVal from(long t, double v) {
            return new TimeVal(t, v);
        }

        static TimeVal from(Metric m) {
            return new TimeVal(m.getTimestamp(), m.getNumberValue().doubleValue());
        }

        TimeVal(long t, double v) {
            this.t = t;
            this.v = v;
        }

        @Override
        public String toString() {
            return "TimeVal{t=" + t + ", v=" + v + '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            TimeVal timeVal = (TimeVal) o;
            return t == timeVal.t && Double.compare(timeVal.v, v) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(t, v);
        }
    }

    void assertMetrics(long step, long heartbeat, List<Metric> input, List<TimeVal> expected) {
        ManualClock clock = new ManualClock(0);
        MemoryMetricObserver mmo = new MemoryMetricObserver("m", 1);
        MetricObserver transform = new NormalizationTransform(mmo, step, heartbeat, clock);

        int i = 0;
        for (Metric m: input) {
            transform.update(ImmutableList.of(m));
            Metric result = mmo.getObservations().get(0).get(0);
            assertEquals(TimeVal.from(result), expected.get(i));
            i++;
        }
    }

    @Test
    public void testBasic() throws Exception {
        List<Metric> inputList = ImmutableList.of(
                newMetric(5, 1.0),
                newMetric(15, 2.0),
                newMetric(25, 2.0),
                newMetric(35, 1.0),
                newMetric(85, 1.0),
                newMetric(95, 2.0),
                newMetric(105, 2.0));
        List<TimeVal> expected = ImmutableList.of(
                TimeVal.from(0, 0.5),
                TimeVal.from(10, 1.5),
                TimeVal.from(20, 2.0),
                TimeVal.from(30, 1.5),
                TimeVal.from(80, 0.5),
                TimeVal.from(90, 1.5),
                TimeVal.from(100, 2.0)
        );

        assertMetrics(10, 20, inputList, expected);
    }

    @Test
    public void testAlreadyNormalized() throws Exception {
        List<Metric> inputList = ImmutableList.of(
                newMetric(0, 10.0),
                newMetric(10, 20.0),
                newMetric(20, 30.0),
                newMetric(30, 10.0));
        List<TimeVal> expected = ImmutableList.of(
                TimeVal.from(0, 10.0),
                TimeVal.from(10, 20.0),
                TimeVal.from(20, 30.0),
                TimeVal.from(30, 10.0));
        assertMetrics(10, 20, inputList, expected);
    }

    @Test
    public void testNormalizedMissedHeartbeat() throws Exception {
        List<Metric> inputList = ImmutableList.of(
                newMetric(0, 10.0),
                newMetric(10, 10.0),
                newMetric(30, 10.0));
        List<TimeVal> expected = ImmutableList.of(
                TimeVal.from(0, 10.0),
                TimeVal.from(10, 10.0),
                TimeVal.from(30, 10.0));
        assertMetrics(10, 20, inputList, expected);
    }

    long t(int m, int s) {
        return (m * 60 + s) * 1000L;
    }

        @Test
        public void testRandomOffset() throws Exception {
            List<Metric> inputList = ImmutableList.of(
                    newMetric(t(1, 13), 1.0),
                    newMetric(t(2, 13), 1.0),
                    newMetric(t(3, 13), 1.0));

            List<TimeVal> expected = ImmutableList.of(
                    TimeVal.from(t(1, 0), 47.0 / 60.0),
                    TimeVal.from(t(2, 0), 1.0),
                    TimeVal.from(t(3, 0), 1.0));

            assertMetrics(60000, 120000, inputList, expected);
        }
    }
