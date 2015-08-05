/*
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.ExpiringCache;
import com.netflix.servo.util.ManualClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class DynamicCounterTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounterTest.class);

  private DynamicCounter getInstance() throws Exception {
    Field theInstance = DynamicCounter.class.getDeclaredField("INSTANCE");
    theInstance.setAccessible(true);
    return (DynamicCounter) theInstance.get(null);
  }

  private List<Monitor<?>> getCounters() throws Exception {
    return getInstance().getMonitors();
  }

  private final TagList tagList = BasicTagList.of("PLATFORM", "true");

  private StepCounter getByName(String name) throws Exception {
    List<Monitor<?>> counters = getCounters();
    for (Monitor<?> m : counters) {
      String monitorName = m.getConfig().getName();
      if (name.equals(monitorName)) {
        return (StepCounter) m;
      }
    }
    return null;
  }

  @Test
  public void testHasRightType() throws Exception {
    DynamicCounter.increment("test1", tagList);
    StepCounter c = getByName("test1");
    assert c != null;
    Tag type = c.getConfig().getTags().getTag(DataSourceType.KEY);
    assertEquals(type.getValue(), "NORMALIZED");
  }

  final ManualClock clock = new ManualClock(0L);

  /**
   * Erase all previous counters by creating a new loading cache with a short expiration time.
   */
  @BeforeMethod
  public void setupInstance() throws Exception {
    LOGGER.info("Setting up DynamicCounter instance with a new cache");
    DynamicCounter theInstance = getInstance();
    Field counters = DynamicCounter.class.getDeclaredField("counters");
    counters.setAccessible(true);
    ExpiringCache<MonitorConfig, Counter> newShortExpiringCache =
        new ExpiringCache<>(60000L, config -> new StepCounter(config, clock), 100L, clock);

    counters.set(theInstance, newShortExpiringCache);
  }

  @Test
  public void testGetValue() throws Exception {
    clock.set(0L);
    DynamicCounter.increment("test1", tagList);
    StepCounter c = getByName("test1");
    clock.set(60000L);
    assert c != null;
    assertEquals(c.getCount(0), 1L);
    c.increment(13);
    clock.set(120000L);
    assertEquals(c.getCount(0), 13L);
  }

  @Test
  public void testExpiration() throws Exception {
    clock.set(0L);
    DynamicCounter.increment("test1", tagList);
    DynamicCounter.increment("test2", tagList);

    clock.set(500L);
    DynamicCounter.increment("test1", tagList);

    clock.set(1000L);
    DynamicCounter.increment("test1", tagList);

    clock.set(60200L);
    StepCounter c1 = getByName("test1");
    assert c1 != null;
    assertEquals(c1.getCount(0), 3L);

    // the expiration task is not using Clock
    Thread.sleep(200);

    StepCounter c2 = getByName("test2");
    assertNull(c2, "Counters not used in a while should expire");
  }

  @Test
  public void testByStrings() throws Exception {
    clock.set(1L);
    DynamicCounter.increment("byName");
    DynamicCounter.increment("byName");
    StepCounter c = getByName("byName");
    clock.set(60001L);
    assert c != null;
    assertEquals(c.getCount(0), 2L);

    DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
    DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
    StepCounter c2 = getByName("byName2");
    clock.set(120001L);
    assert c2 != null;
    assertEquals(c2.getCount(0), 2L);
  }

  @Test
  public void testShouldNotThrow() throws Exception {
    DynamicCounter.increment("name", "", "");
  }
}
