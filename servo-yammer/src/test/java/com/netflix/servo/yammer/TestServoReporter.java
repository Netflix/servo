package com.netflix.servo.yammer;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.BasicCompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class TestServoReporter {
    @Test
    public void test() throws InterruptedException {
        Gauge<Double> doubleGauge = new Gauge<Double>() {
            @Override
            public Double value() {
                return 10.0;
            }
        };
        Gauge<Integer> intGauge = new Gauge<Integer>() {
            @Override
            public Integer value() {
                return 5;
            }
        };
        Gauge<Long> longGauge = new Gauge<Long>() {
            @Override
            public Long value() {
                return 20l;
            }
        };

        Metrics.defaultRegistry().newGauge(new MetricName("test_group", "test_type", "double_gauge"), doubleGauge);
        Metrics.defaultRegistry().newGauge(new MetricName("test_group", "test_type", "int_gauge"), intGauge);
        Metrics.defaultRegistry().newGauge(new MetricName("test_group", "test_type", "long_gauge"), longGauge);

        Histogram histogram = Metrics.defaultRegistry().newHistogram(new MetricName("test_group", "test_type", "test_histogram"), true);
        for (int i = 0; i < 1000; ++i) {
            histogram.update(i);
        }
        Meter meter = Metrics.defaultRegistry().newMeter(new MetricName("test_group", "test_type", "test_meter"), "messages", TimeUnit.SECONDS);
        for (int i = 0; i < 1000; ++i) {
            meter.mark();
        }
        Timer timer = Metrics.defaultRegistry().newTimer(new MetricName("test_group", "test_type", "test_timer"), TimeUnit.MILLISECONDS, TimeUnit.SECONDS);
        TimerContext tc = timer.time();
        Thread.sleep(1234);
        tc.stop();

        ServoReporter reporter = new ServoReporter(Metrics.defaultRegistry(), MetricPredicate.ALL);

        // wait until metrics are collected
        for (int i = 0; i < 10; ++i) {
            reporter.run();
            Thread.sleep(1000);
        }
        reporter.run();
        reporter.shutdown();

        Collection<Monitor<?>> monitors = DefaultMonitorRegistry.getInstance().getRegisteredMonitors();
        assertEquals(monitors.size(), 6);
        int count = 0;
        for (Monitor<?> m : monitors) {
            if (m.getConfig().getName().contains("double_gauge")) {
                assertEquals(((BasicCompositeMonitor) m).getMonitors().get(0).getValue(), 10.0);
                ++count;
            }
            if (m.getConfig().getName().contains("int_gauge")) {
                assertEquals(((BasicCompositeMonitor) m).getMonitors().get(0).getValue(), 5);
                ++count;
            }
            if (m.getConfig().getName().contains("long_gauge")) {
                assertEquals(((BasicCompositeMonitor) m).getMonitors().get(0).getValue(), 20l);
                ++count;
            }
            if (m.getConfig().getName().contains("test_histogram")) {
                int subCount = 0;
                for (Monitor<?> subm : ((BasicCompositeMonitor) m).getMonitors()) {
                    if (subm.getConfig().getName().equals("max")) {
                        assertEquals(subm.getValue(), histogram.max());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("min")) {
                        assertEquals(subm.getValue(), histogram.min());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("mean")) {
                        assertEquals(subm.getValue(), histogram.mean());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("median")) {
                        assertEquals(subm.getValue(), histogram.getSnapshot().getMedian());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("stddev")) {
                        assertEquals(subm.getValue(), histogram.stdDev());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("95")) {
                        assertEquals(subm.getValue(), histogram.getSnapshot().get95thPercentile());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("99")) {
                        assertEquals(subm.getValue(), histogram.getSnapshot().get99thPercentile());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("99.9")) {
                        assertEquals(subm.getValue(), histogram.getSnapshot().get999thPercentile());
                        ++subCount;
                    }
                }
                assertEquals(subCount, 8);
                ++count;
            }
            if (m.getConfig().getName().contains("test_meter")) {
                int subCount = 0;
                for (Monitor<?> subm : ((BasicCompositeMonitor) m).getMonitors()) {
                    if (subm.getConfig().getName().equals("count")) {
                        assertEquals(subm.getValue(), meter.count());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("oneMinuteRate")) {
                        assertEquals(subm.getValue(), meter.oneMinuteRate());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("meanRate")) {
                        assertEquals(((Double) subm.getValue()).doubleValue(), meter.meanRate(), 1);
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("fiveMinuteRate")) {
                        assertEquals(subm.getValue(), meter.fiveMinuteRate());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("fifteenMinuteRate")) {
                        assertEquals(subm.getValue(), meter.fifteenMinuteRate());
                        ++subCount;
                    }
                }
                assertEquals(subCount, 5);
                ++count;
            }
            if (m.getConfig().getName().contains("test_timer")) {
                int subCount = 0;
                for (Monitor<?> subm : ((BasicCompositeMonitor) m).getMonitors()) {
                    if (subm.getConfig().getName().equals("min")) {
                        assertEquals(subm.getValue(), timer.min());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("max")) {
                        assertEquals(subm.getValue(), timer.max());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("mean")) {
                        assertEquals(subm.getValue(), timer.mean());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("median")) {
                        assertEquals(subm.getValue(), timer.getSnapshot().getMedian());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("stddev")) {
                        assertEquals(subm.getValue(), timer.stdDev());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("95")) {
                        assertEquals(subm.getValue(), timer.getSnapshot().get95thPercentile());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("99")) {
                        assertEquals(subm.getValue(), timer.getSnapshot().get99thPercentile());
                        ++subCount;
                    }
                    if (subm.getConfig().getName().equals("99.9")) {
                        assertEquals(subm.getValue(), timer.getSnapshot().get999thPercentile());
                        ++subCount;
                    }
                }
                assertEquals(subCount, 8);
                ++count;
            }
        }

        assertEquals(count, 6);
    }
}
