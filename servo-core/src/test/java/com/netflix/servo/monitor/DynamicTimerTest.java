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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class DynamicTimerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTimerTest.class);
    private DynamicTimer getInstance() throws Exception  {
        Field theInstance = DynamicTimer.class.getDeclaredField("INSTANCE");
        theInstance.setAccessible(true);
        return (DynamicTimer) theInstance.get(null);
    }

    private List<Monitor<?>> getTimers() throws Exception {
        return getInstance().getMonitors();
    }

    private final TagList tagList = new BasicTagList(ImmutableList.of(
      (Tag) new BasicTag("PLATFORM", "true")));

    private Timer getByName(String name) throws Exception {
        List<Monitor<?>> timers = getTimers();
        for (Monitor<?> m : timers) {
            String monitorName = m.getConfig().getName();
            if (name.equals(monitorName)) {
                return (Timer) m;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHasUnitTag() throws Exception {
        DynamicTimer.start("test1", tagList);
        CompositeMonitor c = (CompositeMonitor<Long>) getByName("test1");
        List<Monitor<?>> monitors = c.getMonitors();
        for (Monitor<?> m: monitors) {
            Tag type = m.getConfig().getTags().getTag("unit");
            assertEquals(type.getValue(), "MILLISECONDS");
        }
    }

    /**
     * Erase all previous timers by creating a new loading cache with a short expiration time
     */
    @BeforeMethod
    public void setupInstance() throws Exception {
        LOGGER.info("Setting up DynamicTimer instance with a new cache");
        DynamicTimer theInstance = getInstance();
        Field timers = DynamicTimer.class.getDeclaredField("timers");
        timers.setAccessible(true);
        LoadingCache<DynamicTimer.ConfigUnit, Timer> newShortExpiringCache =
            CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<DynamicTimer.ConfigUnit, Timer>() {
                    @Override
                    public Timer load(final DynamicTimer.ConfigUnit configUnit) throws Exception {
                        return new BasicTimer(configUnit.config, configUnit.unit);
                    }
                });

        timers.set(theInstance, newShortExpiringCache);
    }

    @Test
    public void testGetValue() throws Exception {
        Stopwatch s = DynamicTimer.start("test1", tagList);
        Timer c = getByName("test1");
        s.stop();
        // we don't call s.stop(), so we only have one recorded value
        assertEquals(c.getValue().longValue(), s.getDuration(TimeUnit.MILLISECONDS));
        c.record(13);

        long expected = (13 + s.getDuration(TimeUnit.MILLISECONDS)) / 2;
        assertEquals(c.getValue().longValue(), expected);
    }

    @Test
    public void testExpiration() throws Exception {
        DynamicTimer.start("test1", tagList);
        DynamicTimer.start("test2", tagList);

        Thread.sleep(500L);

        DynamicTimer.start("test1", tagList);
        Thread.sleep(500L);

        Stopwatch s = DynamicTimer.start("test1", tagList);
        Thread.sleep(200L);
        s.stop();
        Timer c1 = getByName("test1");
        assertEquals(c1.getValue().longValue(), s.getDuration(TimeUnit.MILLISECONDS));

        Timer c2 = getByName("test2");
        assertNull(c2, "Timers not used in a while should expire");
    }

    @Test
    public void testByStrings() throws Exception {
        Stopwatch s = DynamicTimer.start("byName");
        Stopwatch s2 = DynamicTimer.start("byName2", "key", "value");

        Thread.sleep(100L);

        s.stop();
        s2.stop();

        Timer c1 = getByName("byName");
        assertEquals(c1.getValue().longValue(), s.getDuration(TimeUnit.MILLISECONDS));

        Timer c2 = getByName("byName2");
        assertEquals(c2.getValue().longValue(), s2.getDuration(TimeUnit.MILLISECONDS));
    }
}
