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

import static com.netflix.servo.BasicTagList.EMPTY;
import static com.netflix.servo.publish.BasicMetricFilter.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.Metric;
import com.netflix.servo.TagList;

import com.netflix.servo.annotations.DataSourceType;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import javax.management.ObjectName;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class JmxMetricPollerTest {

    @Test
    public void testBasic() throws Exception {
        MetricPoller poller = new JmxMetricPoller(
            new LocalJmxConnector(),
            new ObjectName("java.lang:type=OperatingSystem"), 
            MATCH_NONE);

        boolean found = false;
        List<Metric> metrics = poller.poll(MATCH_ALL);
        for (Metric m : metrics) {
            if ("AvailableProcessors".equals(m.getConfig().getName())) {
                found = true;
                Map<String,String> tags = m.getConfig().getTags().asMap();
                assertEquals(tags.get("JmxDomain"), "java.lang");
                assertEquals(tags.get("Jmx.type"), "OperatingSystem");
                assertEquals(tags.get("ClassName"),
                    "com.netflix.servo.publish.JmxMetricPoller");
                assertEquals(tags.get(DataSourceType.KEY), "GAUGE");
            }
        }
        assertTrue(found);
    }

    @Test
    public void testCounterFilter() throws Exception {
        MetricPoller poller = new JmxMetricPoller(
            new LocalJmxConnector(),
            new ObjectName("java.lang:type=OperatingSystem"), 
            MATCH_ALL);

        boolean found = false;
        List<Metric> metrics = poller.poll(MATCH_ALL);
        for (Metric m : metrics) {
            if ("AvailableProcessors".equals(m.getConfig().getName())) {
                found = true;
                Map<String,String> tags = m.getConfig().getTags().asMap();
                assertEquals(tags.get("JmxDomain"), "java.lang");
                assertEquals(tags.get("Jmx.type"), "OperatingSystem");
                assertEquals(tags.get("ClassName"),
                    "com.netflix.servo.publish.JmxMetricPoller");
                assertEquals(tags.get(DataSourceType.KEY), "COUNTER");
            }
        }
        assertTrue(found);
    }

}
