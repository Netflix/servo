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

import com.google.common.collect.Maps;

import com.netflix.servo.publish.BasicMetricFilter;
import com.netflix.servo.publish.CounterToRateMetricTransform;
import com.netflix.servo.publish.FileMetricObserver;
import com.netflix.servo.publish.JmxMetricPoller;
import com.netflix.servo.publish.LocalJmxConnector;
import com.netflix.servo.publish.MetricFilter;
import com.netflix.servo.publish.MetricObserver;
import com.netflix.servo.publish.MetricPoller;
import com.netflix.servo.publish.PollRunnable;
import com.netflix.servo.publish.PollScheduler;
import com.netflix.servo.publish.PrefixMetricFilter;
import com.netflix.servo.publish.RegexMetricFilter;

import javax.management.ObjectName;
import java.io.File;
import java.util.NavigableMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Example of collecting arbitrary JMX metrics, in this case the standard
 * metrics exposed under java.lang by the JVM.
 */
public final class JvmMetricExample {

    private JvmMetricExample() {
    }

    public static void main(String[] args) throws Exception {
        // Filter used to identify metrics that are counters
        NavigableMap<String, MetricFilter> counters = Maps.newTreeMap();

        // ClassLoadingMXBean
        counters.put("LoadedClassCount", BasicMetricFilter.MATCH_ALL);
        counters.put("TotalLoadedClassCount", BasicMetricFilter.MATCH_ALL);
        counters.put("UnloadedClassCount", BasicMetricFilter.MATCH_ALL);

        // CompilationMXBean
        counters.put("TotalCompilationTime", BasicMetricFilter.MATCH_ALL);

        // GarbageCollectorMXBean
        counters.put("CollectionCount", BasicMetricFilter.MATCH_ALL);
        counters.put("CollectionTime", BasicMetricFilter.MATCH_ALL);

        // MemoryPoolMXBean
        counters.put("CollectionUsageThresholdCount", BasicMetricFilter.MATCH_ALL);
        counters.put("UsageThresholdCount", BasicMetricFilter.MATCH_ALL);

        // RuntimeMXBean
        counters.put("Uptime", BasicMetricFilter.MATCH_ALL);

        // ThreadMXBean
        counters.put("TotalStartedThreadCount", BasicMetricFilter.MATCH_ALL);

        // Create prefix filter on the metric name, default to match none if
        // no match is found so that by default metrics will be GAUGEs
        MetricFilter counterFilter = new PrefixMetricFilter(
            null,                         // Tag key, null means use metric name
            BasicMetricFilter.MATCH_NONE, // Root filter if no better match
            counters);                    // Specific filters

        // Create a new poller for the local JMX server that queries all
        // metrics from the java.lang domain
        MetricPoller poller = new JmxMetricPoller(
            new LocalJmxConnector(),
            new ObjectName("java.lang:type=*,*"),
            counterFilter);


        // Filter to restrict the set of metrics returned, in this case ignore
        // many of the flags indicating whether or not certain features are
        // suported or enabled
        MetricFilter filter = new RegexMetricFilter(
            null,    // Tag key, null means use metric name
            Pattern.compile(".*Supported$|.*Enabled$|^Valid$|^Verbose$"),
            false,   // Match if the tag is missing
            true);   // Invert the pattern match

        // Create a new observer that records observations to files in the
        // current working directory
        MetricObserver observer = new FileMetricObserver(
            "jvmstats", new File("."));

        // Sampling interval
        final long samplingInterval = 10;
        TimeUnit samplingUnit = TimeUnit.SECONDS;

        // Transform used to convert counter metrics into a rate per second
        MetricObserver transform = new CounterToRateMetricTransform(
            observer, 2 * samplingInterval, samplingUnit);

        // Schedule metrics to be collected in the background every 10 seconds
        PollRunnable task = new PollRunnable(poller, filter, transform);
        PollScheduler scheduler = PollScheduler.getInstance();
        scheduler.start();
        scheduler.addPoller(task, samplingInterval, samplingUnit);

        // Do main work of program
        while (true) {
            System.out.println("Doing work...");
            Thread.sleep(samplingUnit.toMillis(samplingInterval));
        }
    }
}
