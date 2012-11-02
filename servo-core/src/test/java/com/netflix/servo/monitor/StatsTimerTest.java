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
    static Logger LOGGER = LoggerFactory.getLogger(StatsTimerTest.class);

    @Override
    public StatsTimer newInstance(String name) {
        final double [] percentiles = { 50.0, 95.0, 99.0, 99.5 };
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
    public void testStats() throws Exception {
        final StatsTimer timer = newInstance("t1");
        final Map<String, Number> expectedValues = Maps.newHashMap();
        final int N = 200 * 1000;
        expectedValues.put("count", (long) N);
        expectedValues.put("totalTime", (long) N * (N - 1) / 2);
        expectedValues.put("stdDev", 57735.17);
        expectedValues.put("percentile_50", 100 * 1000.0);
        expectedValues.put("percentile_95", 190 * 1000.0);
        expectedValues.put("percentile_99", 198 * 1000.0);
        expectedValues.put("percentile_99_5", 199 * 1000.0);

        for (int i = 0; i < N; ++i) {
            timer.record(i);
        }

        Thread.sleep(1000L);
        assertStats(timer.getMonitors(), expectedValues);
    }

    private void assertStats(List<Monitor<?>> monitors, Map<String, Number> expectedValues) {
        for (Monitor<?> monitor : monitors) {
            final String stat = monitor.getConfig().getTags().getValue("statistic");
            final Number actual = (Number)monitor.getValue();
            final Number expected = expectedValues.get(stat);
            if (expected instanceof Double) {
                double e = (Double)expected;
                double a = (Double)actual;
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
        final int N = 100 * 1000;
        expectedValues.put("count", (long) N);
        expectedValues.put("totalTime", (long) N * (N - 1) / 2);
        expectedValues.put("stdDev", 28867.66);
        expectedValues.put("percentile_50", 50 * 1000.0);
        expectedValues.put("percentile_95", 95 * 1000.0);
        expectedValues.put("percentile_99", 99 * 1000.0);
        expectedValues.put("percentile_99_5", 99.5 * 1000.0);

        ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < N; ++i) {
            service.submit(new TimerTask(timer, i));
        }

        Thread.sleep(1000L);
        assertStats(timer.getMonitors(), expectedValues);
    }

}
