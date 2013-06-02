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
package com.netflix.servo.publish;

import com.netflix.servo.BasicMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.monitor.AbstractMonitor;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import org.testng.annotations.Test;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import java.util.concurrent.atomic.AtomicLong;

import static com.netflix.servo.publish.BasicMetricFilter.MATCH_ALL;
import static org.testng.Assert.*;

public class MonitorRegistryMetricPollerTest {
    private static final long TEN_SECONDS = 10 * 1000;
    private static final long ONE_MINUTE = 60 * 1000;
    private static final long ONE_HOUR = 60 * ONE_MINUTE;

    @Test
    public void testBasic() throws Exception {
        MonitorRegistry registry = new BasicMonitorRegistry();
        registry.register(Monitors.newCounter("test"));

        MetricPoller poller = new MonitorRegistryMetricPoller(registry);
        Metric metric = poller.poll(MATCH_ALL).get(0);
        MonitorConfig expected = MonitorConfig.builder("test")
            .withTag(DataSourceType.COUNTER)
            .build();
        assertEquals(metric.getConfig(), expected);
    }

    @Test
    public void testSlowMonitor() throws Exception {
        MonitorRegistry registry = new BasicMonitorRegistry();
        registry.register(new SlowCounter("slow"));
        registry.register(Monitors.newCounter("test"));

        MetricPoller poller = new MonitorRegistryMetricPoller(registry);
        long start = System.currentTimeMillis();
        Metric metric = poller.poll(MATCH_ALL).get(0);
        long end = System.currentTimeMillis();

        // Verify we didn't wait too long, we should only wait 1 second but allow up to
        // 10 to make it less likely to have spurious test failures
        assertTrue(end - start < TEN_SECONDS);

        MonitorConfig expected = MonitorConfig.builder("test")
            .withTag(DataSourceType.COUNTER)
            .build();
        assertEquals(metric.getConfig(), expected);
    }

    @Test
    public void testShutdown() throws Exception {
        MonitorRegistry registry = new BasicMonitorRegistry();
        registry.register(Monitors.newCounter("test"));

        final String threadPrefix = "ServoMonitorGetValueLimiter";

        int baseCount = countThreadsWithName(threadPrefix);
        MonitorRegistryMetricPoller[] pollers = new MonitorRegistryMetricPoller[10];
        for (int i = 0; i < pollers.length; ++i) {
            pollers[i] = new MonitorRegistryMetricPoller(registry);
            pollers[i].poll(MATCH_ALL);
        }
        assertTrue(countThreadsWithName(threadPrefix) >= 10 + baseCount);

        for (MonitorRegistryMetricPoller poller : pollers) {
            poller.shutdown();
        }
        Thread.sleep(1000);
        assertTrue(countThreadsWithName(threadPrefix) <= baseCount);

        // Verify threads will be cleanup up by gc
        /*System.err.println("pre-gc: " + countThreadsWithName("ServoMonitorGetValueLimiter"));
        for (int i = 0; i < pollers.length; ++i) {
            pollers[i] = null;
        }
        System.gc();
        Thread.sleep(1000);
        System.err.println("post-gc: " + countThreadsWithName("ServoMonitorGetValueLimiter"));*/

    }

    private int countThreadsWithName(String prefix) {
        final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        final long[] ids = bean.getAllThreadIds();
        final ThreadInfo[] infos = bean.getThreadInfo(ids);
        int count = 0;
        for (ThreadInfo info : infos) {
            if (info != null && info.getThreadName().startsWith(prefix)) {
                ++count;
            }
        }
        return count;
    }

    private static class SlowCounter extends AbstractMonitor<Number> implements Counter {
        private final AtomicLong count = new AtomicLong();

        public SlowCounter(String name) {
            super(MonitorConfig.builder(name).withTag(DataSourceType.COUNTER).build());
        }

        @Override
        public void increment() {
            count.incrementAndGet();
        }

        @Override
        public void increment(long amount) {
            count.getAndAdd(amount);
        }

        @Override
        public Number getValue() {
            try {
                Thread.sleep(ONE_HOUR);
            } catch (Exception e) {
                System.err.println("Ignoring exception " + e.getMessage());
            }
            return count.get();
        }
    }
}
