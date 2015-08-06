/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish;

import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.Tags;
import org.testng.annotations.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
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

  /**
   * Tabular JMX values are very useful for cases where we want the same behavior as CompositePath but we
   * don't know up front what the values are going to be.
   */
  @Test
  public void testTabularData() throws Exception {

    MapMXBean mapMXBean = new MapMXBean();
    try {
      MetricPoller poller = new JmxMetricPoller(
          new LocalJmxConnector(),
          new ObjectName("com.netflix.servo.test:*"),
          MATCH_ALL);

      List<Metric> metrics = poller.poll(config -> config.getName().equals("Count"));
      assertEquals(metrics.size(), 2);
      Map<String, Integer> values = new HashMap<>();
      for (Metric m : metrics) {
        values.put(m.getConfig().getTags().getTag("JmxCompositePath").getValue(), (Integer) m.getValue());
      }
      assertEquals(values.get("Entry1"), (Integer) 111);
      assertEquals(values.get("Entry2"), (Integer) 222);
    } finally {
      mapMXBean.destroy();
    }
  }

  public interface TestMapMXBean {
    Map<String, Integer> getCount();

    String getStringValue();
  }

  public static class MapMXBean implements TestMapMXBean {

    private final ObjectName objectName;

    MapMXBean() throws Exception {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      objectName = new ObjectName("com.netflix.servo.test", "Test", "Obj");
      destroy();
      mbs.registerMBean(this, objectName);
    }

    public void destroy() throws Exception {
      MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
      if (mbs.isRegistered(objectName)) {
        mbs.unregisterMBean(objectName);
      }
    }

    @Override
    public Map<String, Integer> getCount() {
      Map<String, Integer> map = new HashMap<>();
      map.put("Entry1", 111);
      map.put("Entry2", 222);
      return map;
    }

    @Override
    public String getStringValue() {
      return "AStringResult";
    }
  }

  @Test
  public void testDefaultTags() throws Exception {
    MetricPoller poller = new JmxMetricPoller(
        new LocalJmxConnector(),
        Collections.singletonList(new ObjectName("java.lang:type=OperatingSystem")),
        MATCH_ALL,
        true,
        Collections.singletonList(Tags.newTag("HostName", "localhost")));

    List<Metric> metrics = poller.poll(MATCH_ALL);
    for (Metric m : metrics) {
      Map<String, String> tags = m.getConfig().getTags().asMap();
      assertEquals(tags.get("HostName"), "localhost");
    }
  }

  @Test
  public void testNonNumericMetrics() throws Exception {
    MapMXBean mapMXBean = new MapMXBean();
    try {
      MetricPoller poller = new JmxMetricPoller(
          new LocalJmxConnector(),
          Collections.singletonList(new ObjectName("com.netflix.servo.test:*")),
          MATCH_ALL,
          false,
          null);

      List<Metric> metrics = poller.poll(config -> config.getName().equals("StringValue"));
      assertEquals(metrics.size(), 1);
      assertEquals(metrics.get(0).getValue(), "AStringResult");
    } finally {
      mapMXBean.destroy();
    }
  }
}
