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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.netflix.servo.annotations.DataSourceType.COUNTER;
import static com.netflix.servo.annotations.DataSourceType.GAUGE;
import static com.netflix.servo.annotations.DataSourceType.INFORMATIONAL;
import static org.testng.Assert.assertEquals;

public class AnnotationsTest {
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SS_SHOULD_BE_STATIC",
            justification = "Values used through reflection")
    static class Metrics {
        @com.netflix.servo.annotations.Monitor(type = GAUGE)
        private final AtomicLong annoGauge = new AtomicLong(0L);

        @com.netflix.servo.annotations.Monitor(type = COUNTER)
        public final AtomicLong annoCounter = new AtomicLong(0L);

        @com.netflix.servo.annotations.Monitor(type = GAUGE)
        public final long primitiveGauge = 0L;

        @com.netflix.servo.annotations.Monitor(type = INFORMATIONAL)
        private String annoInfo() {
            return "foo";
        }
    }

    @Test
    public void testDefaultNames() throws Exception {
        Metrics m = new Metrics();
        List<Monitor<?>> monitors = Lists.newArrayList();
        Monitors.addAnnotatedFields(monitors, null, null, m, m.getClass());

        List<String> expectedNames = ImmutableList.of(
            "annoCounter", "annoGauge", "annoInfo", "primitiveGauge");
        List<String> actualNames = Lists.newArrayList();
        for (Monitor<?> monitor: monitors) {
            actualNames.add(monitor.getConfig().getName());
        }
        Collections.sort(actualNames);
        assertEquals(actualNames, expectedNames);
    }
}
