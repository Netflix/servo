/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
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
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

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

    private static class SlowCounter extends AbstractMonitor<Long> implements Counter {
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
        public Long getValue() {
            try { Thread.sleep(ONE_HOUR); } catch (Exception e) { }
            return count.get();
        }
    }
}
