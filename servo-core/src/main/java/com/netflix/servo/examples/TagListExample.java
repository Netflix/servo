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
import com.netflix.servo.tag.*;

import com.netflix.servo.util.ThreadCpuStats;
import com.netflix.servo.util.ThreadCpuStats.CpuUsage;

import java.util.concurrent.TimeUnit;

import java.util.List;
import java.util.Random;

/**
 * Creates a lot of tag lists with a fixed set of key/value pairs. Mostly used for running under a
 * profiler.
 */
public class TagListExample {

    private static final Random RANDOM = new Random(42);

    private static final char[] ALLOWED_CHARS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        '-', '_', '.'
    };

    private TagListExample() {
    }

    private static String randomString(int n) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < n; ++i) {
            int c = RANDOM.nextInt(ALLOWED_CHARS.length);
            buf.append(ALLOWED_CHARS[c]);
        }
        return buf.toString();
    }

    private static Tag[] randomTags(int n) {
        Tag[] tags = new Tag[n];
        for (int i = 0; i < n; ++i) {
            String key = randomString(RANDOM.nextInt(25) + 5);
            String value = randomString(RANDOM.nextInt(100) + 5);
            tags[i] = Tags.newTag(key, value); //new BasicTag(key, value);
        }
        return tags;
    }

    private static int doTest(Tag[] tags, int n) {
        int total = 0;
        for (int i = 0; i < n; ++i) {
            MonitorConfig.Builder builder = MonitorConfig.builder("metricName");
            for (int j = 0; j < RANDOM.nextInt(25); ++j) {
                Tag t = tags[RANDOM.nextInt(tags.length)];
                //builder.withTag(new String(t.getKey()), new String(t.getValue()));
                //builder.withTag(t.getKey(), t.getValue());
                builder.withTag(t);
            }
            total += builder.build().getTags().size();
        }
        return total;
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: TagListExample <numTags> <numTests>");
            System.exit(1);
        }
        final int numTags = Integer.valueOf(args[0]);
        final int numTests = Integer.valueOf(args[1]);

        // Actual runs, keep looping
        final Tag[] tags = randomTags(numTags);
        for (int i = 0;; ++i) {
            long start = System.nanoTime();
            int size = doTest(tags, numTests);
            long end = System.nanoTime();
            System.out.println(((end - start) / 1000) + " microseconds, " + size + " tags");
        }
    }
}
