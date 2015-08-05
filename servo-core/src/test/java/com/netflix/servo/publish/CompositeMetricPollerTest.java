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
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.util.Iterables;
import com.netflix.servo.util.UnmodifiableList;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.netflix.servo.publish.BasicMetricFilter.MATCH_ALL;
import static org.testng.Assert.assertEquals;

public class CompositeMetricPollerTest {

  private List<Metric> mkList() {
    return UnmodifiableList.of(
        new Metric("m1", SortedTagList.EMPTY, 0L, 0),
        new Metric("m2", SortedTagList.builder().withTag("c", "a.b.c.d.M1").build(), 0L, 0),
        new Metric("m3", SortedTagList.builder().withTag("c", "a.b.c.c.M3").build(), 0L, 0),
        new Metric("m4", SortedTagList.builder().withTag("c", "a.b.c.d.M4").build(), 0L, 0),
        new Metric("m5", SortedTagList.builder().withTag("c", "a.a.a.a.M5").build(), 0L, 0)
    );
  }

  private MetricPoller newPoller(long delay) {
    MockMetricPoller poller = new MockMetricPoller();
    poller.setMetrics(mkList());
    poller.setDelay(delay);
    return poller;
  }

  @Test
  public void testBasic() throws Exception {
    Map<String, MetricPoller> pollers = new HashMap<>();
    pollers.put("p1", newPoller(0));

    ExecutorService exec = Executors.newFixedThreadPool(1);
    MetricPoller poller = new CompositeMetricPoller(pollers, exec, 10000);

    assertEquals(poller.poll(MATCH_ALL), mkList());
  }

  @Test
  public void testMultiple() throws Exception {
    Map<String, MetricPoller> pollers = new HashMap<>();
    pollers.put("p1", newPoller(0));
    pollers.put("p2", newPoller(0));

    ExecutorService exec = Executors.newFixedThreadPool(1);
    MetricPoller poller = new CompositeMetricPoller(pollers, exec, 10000);

    assertEquals(poller.poll(MATCH_ALL),
        UnmodifiableList.copyOf(Iterables.concat(mkList(), mkList())));
  }

  @Test
  public void testTimeout() throws Exception {
    // Note: java8 changes the hashing/traversal order so you can see slightly different
    // behavior if there is a bug. In particular before we were cancelling the future on
    // timeout, then if the p2 key times out it will fail on java7 and pass on java8.
    // If the p1 key times out the opposite.
    Map<String, MetricPoller> pollers = new HashMap<>();
    pollers.put("p1", newPoller(120000));
    pollers.put("p2", newPoller(0));

    // To make it fail regardless of jdk version if there is an issue
    pollers.put("p3", newPoller(120000));
    pollers.put("p4", newPoller(120000));
    pollers.put("p5", newPoller(120000));

    ExecutorService exec = Executors.newFixedThreadPool(1);
    MetricPoller poller = new CompositeMetricPoller(pollers, exec, 500);
    assertEquals(poller.poll(MATCH_ALL), mkList());
  }

  @Test
  public void testException() throws Exception {
    MockMetricPoller mock = new MockMetricPoller();
    mock.setDie(true);

    Map<String, MetricPoller> pollers = new HashMap<>();
    pollers.put("p1", mock);
    pollers.put("p2", newPoller(0));

    ExecutorService exec = Executors.newFixedThreadPool(1);
    MetricPoller poller = new CompositeMetricPoller(pollers, exec, 10000);

    assertEquals(poller.poll(MATCH_ALL), mkList());
  }
}
