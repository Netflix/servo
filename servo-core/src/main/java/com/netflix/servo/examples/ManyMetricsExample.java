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
package com.netflix.servo.examples;

import com.google.common.collect.Lists;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;

import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.MemoryMetricObserver;
import com.netflix.servo.publish.MetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MonitorRegistryMetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;

import com.netflix.servo.util.ThreadCpuStats;
import com.netflix.servo.util.ThreadCpuStats.CpuUsage;

import java.util.concurrent.TimeUnit;

import java.util.List;

/**
 * Registers a lot of metrics and configures a poller to query them once a second. Mostly used
 * for running under a profiler.
 */
public class ManyMetricsExample {

    private ManyMetricsExample() {
    }

    private static Counter newCounter(int tagsPerMetric, int i) {
        final MonitorConfig cfg = MonitorConfig.builder("nameOfCounter." + i)
            .withTag("class", ManyMetricsExample.class.getSimpleName())
            .withTag("id", "idForCounter." + i)
            .build();
        return new BasicCounter(cfg);
    }

    private static void startPolling() {
        PollScheduler scheduler = PollScheduler.getInstance();
        scheduler.start();

        final int heartbeatInterval = 20;
        MetricObserver transform = new CounterToRateMetricTransform(
            new MemoryMetricObserver("test", 1),
            heartbeatInterval,
            TimeUnit.SECONDS);

        MetricFilter filter = new MetricFilter() {
            public boolean matches(MonitorConfig config) {
                final String id = config.getTags().getValue("id");
                return id != null && id.endsWith("0");
            }
        };

        PollRunnable task = new PollRunnable(
            new MonitorRegistryMetricPoller(),
            filter,
            transform);

        final int samplingInterval = 1;
        scheduler.addPoller(task, samplingInterval, TimeUnit.SECONDS);
    }

    public static void main(String[] args) throws Exception {
        //DefaultMonitorRegistry.getInstance().register(counter);

        if (args.length != 2) {
            System.out.println("Usage: ManyMetricsExample <tagsPerMetric> <numMetrics>");
            System.exit(1);
        }
        final int tagsPerMetric = Integer.valueOf(args[0]);
        final int numMetrics = Integer.valueOf(args[1]);

        final List<Counter> counters = Lists.newArrayList();
        for (int i = 0; i < numMetrics; ++i) {
            final Counter c = newCounter(tagsPerMetric, i);
            counters.add(c);
            DefaultMonitorRegistry.getInstance().register(c);
        }

        startPolling();

        final ThreadCpuStats stats = ThreadCpuStats.getInstance();
        stats.start();

        // Update the counters once in a while
        final Timer t = Monitors.newTimer("updateCounts");
        DefaultMonitorRegistry.getInstance().register(t);
        final long delay = 500L;
        final int report = 120;
        int count = 0;
        while (true) {
            final Stopwatch s = t.start();
            try {
                for (Counter c : counters) {
                    c.increment();
                }

                if (count % report == 0) {
                    stats.printThreadCpuUsages();
                }

                Thread.sleep(delay);
                ++count;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                s.stop();
            }
        }
    }
}
