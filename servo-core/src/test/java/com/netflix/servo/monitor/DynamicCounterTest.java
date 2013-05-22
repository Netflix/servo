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

public class DynamicCounterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounterTest.class);
    private DynamicCounter getInstance() throws Exception  {
        Field theInstance = DynamicCounter.class.getDeclaredField("INSTANCE");
        theInstance.setAccessible(true);
        return (DynamicCounter) theInstance.get(null);
    }

    private List<Monitor<?>> getCounters() throws Exception {
        return getInstance().getMonitors();
    }

    private final TagList tagList = new BasicTagList(ImmutableList.of(
        (Tag) new BasicTag("PLATFORM", "true")));

    private Counter getByName(String name) throws Exception {
        List<Monitor<?>> counters = getCounters();
        for (Monitor<?> m : counters) {
            String monitorName = m.getConfig().getName();
            if (name.equals(monitorName)) {
                return (Counter) m;
            }
        }
        return null;
    }

    @Test
    public void testHasCounterTag() throws Exception {
        DynamicCounter.increment("test1", tagList);
        Counter c = getByName("test1");
        Tag type = c.getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "COUNTER");
    }

    /**
     * Erase all previous counters by creating a new loading cache with a short expiration time
     */
    @BeforeMethod
    public void setupInstance() throws Exception {
        LOGGER.info("Setting up DynamicCounter instance with a new cache");
        DynamicCounter theInstance = getInstance();
        Field counters = DynamicCounter.class.getDeclaredField("counters");
        counters.setAccessible(true);
        LoadingCache<MonitorConfig, Counter> newShortExpiringCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<MonitorConfig, Counter>() {
                    @Override
                    public Counter load(final MonitorConfig config) throws Exception {
                        return new BasicCounter(config);
                    }
                });

        counters.set(theInstance, newShortExpiringCache);
    }

    @Test
    public void testGetValue() throws Exception {
        DynamicCounter.increment("test1", tagList);
        Counter c = getByName("test1");
        assertEquals(c.getValue().longValue(), 1L);
        c.increment(13);
        assertEquals(c.getValue().longValue(), 14L);
    }

    @Test
    public void testExpiration() throws Exception {
        DynamicCounter.increment("test1", tagList);
        DynamicCounter.increment("test2", tagList);

        Thread.sleep(500L);
        DynamicCounter.increment("test1", tagList);

        Thread.sleep(500L);
        DynamicCounter.increment("test1", tagList);

        Thread.sleep(200L);
        Counter c1 = getByName("test1");
        assertEquals(c1.getValue().longValue(), 3L);

        Counter c2 = getByName("test2");
        assertNull(c2, "Counters not used in a while should expire");
    }

    @Test
    public void testByStrings() throws Exception {
        DynamicCounter.increment("byName");
        DynamicCounter.increment("byName");

        Counter c = getByName("byName");
        assertEquals(c.getValue().longValue(), 2L);

        DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
        DynamicCounter.increment("byName2", "key", "value", "key2", "value2");
        Counter c2 = getByName("byName2");
        assertEquals(c2.getValue().longValue(), 2L);
    }
}
