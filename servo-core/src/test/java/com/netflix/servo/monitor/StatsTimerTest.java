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

import com.google.common.collect.Maps;
import com.netflix.servo.stats.StatsConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.testng.Assert.assertEquals;

public class StatsTimerTest extends AbstractMonitorTest<StatsTimer> {
    static final Logger LOGGER = LoggerFactory.getLogger(StatsTimerTest.class);

    @Override
    public StatsTimer newInstance(String name) {
        final double [] percentiles = {50.0, 95.0, 99.0, 99.5};
        final StatsConfig statsConfig = new StatsConfig.Builder()
                .withSampleSize(200000)
                .withPercentiles(percentiles)
                .withPublishStdDev(true)
                .withComputeFrequencyMillis(1000)
                .build();
        final MonitorConfig config = MonitorConfig.builder(name).build();
        return new StatsTimer(config, statsConfig);
    }

    @Test
    public void testNoRecordedValues() throws Exception {
        final StatsTimer timer = newInstance("novalue");
        assertEquals(timer.getCount(), 0L);
        assertEquals(timer.getTotalTime(), 0L);
        final long timerValue = timer.getValue();
        assertEquals(timerValue, 0L);
    }

    @Test
    public void testStats() throws Exception {
        final StatsTimer timer = newInstance("t1");
        final Map<String, Number> expectedValues = Maps.newHashMap();
        final int n = 200 * 1000;
        expectedValues.put("count", (long) n);
        expectedValues.put("totalTime", (long) n * (n - 1) / 2);
        expectedValues.put("stdDev", 57735.17);
        expectedValues.put("percentile_50", 100 * 1000.0);
        expectedValues.put("percentile_95", 190 * 1000.0);
        expectedValues.put("percentile_99", 198 * 1000.0);
        expectedValues.put("percentile_99.50", 199 * 1000.0);

        for (int i = 0; i < n; ++i) {
            timer.record(i);
        }

        Thread.sleep(1000L);
        assertStats(timer.getMonitors(), expectedValues);
    }

    private void assertStats(List<Monitor<?>> monitors, Map<String, Number> expectedValues) {
        for (Monitor<?> monitor : monitors) {
            final String stat = monitor.getConfig().getTags().getValue("statistic");
            final Number actual = (Number) monitor.getValue();
            final Number expected = expectedValues.get(stat);
            if (expected instanceof Double) {
                double e = (Double) expected;
                double a = (Double) actual;
                assertEquals(a, e, 0.5, stat);
            } else {
                assertEquals(actual, expected, stat);
            }
        }
    }

    private static class TimerTask implements Runnable {
        final StatsTimer timer;
        final int value;
        TimerTask(StatsTimer timer, int value) {
            this.timer = timer;
            this.value = value;
        }
        @Override
        public void run() {
            timer.record(value);
        }
    }

    @Test
    public void testMultiThreadStats() throws Exception {
        final StatsTimer timer = newInstance("t1");
        final Map<String, Number> expectedValues = Maps.newHashMap();
        final int n = 10 * 1000;
        expectedValues.put("count", (long) n);
        expectedValues.put("totalTime", (long) n * (n - 1) / 2);
        expectedValues.put("stdDev", 2886.766);
        expectedValues.put("percentile_50", 5 * 1000.0);
        expectedValues.put("percentile_95", 9.5 * 1000.0);
        expectedValues.put("percentile_99", 9.9 * 1000.0);
        expectedValues.put("percentile_99.50", 9.95 * 1000.0);

        final int poolSize = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < n; ++i) {
            service.submit(new TimerTask(timer, i));
        }

        Thread.sleep(1000L);
        assertStats(timer.getMonitors(), expectedValues);
    }

}
