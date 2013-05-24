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

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import org.testng.annotations.Test;

import javax.management.ObjectName;
import java.util.List;
import java.util.Map;

import static com.netflix.servo.publish.BasicMetricFilter.MATCH_ALL;
import static com.netflix.servo.publish.BasicMetricFilter.MATCH_NONE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
                Map<String, String> tags = m.getConfig().getTags().asMap();
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
                Map<String, String> tags = m.getConfig().getTags().asMap();
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
