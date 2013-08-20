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

public class DynamicGaugeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicGaugeTest.class);
    private DynamicGauge getInstance() throws Exception  {
        Field theInstance = DynamicGauge.class.getDeclaredField("INSTANCE");
        theInstance.setAccessible(true);
        return (DynamicGauge) theInstance.get(null);
    }

    private List<Monitor<?>> getGauges() throws Exception {
        return getInstance().getMonitors();
    }

    private final TagList tagList = new BasicTagList(ImmutableList.of(
        (Tag) new BasicTag("PLATFORM", "true")));

    private DoubleGauge getByName(String name) throws Exception {
        List<Monitor<?>> gauges = getGauges();
        for (Monitor<?> m : gauges) {
            String monitorName = m.getConfig().getName();
            if (name.equals(monitorName)) {
                return (DoubleGauge) m;
            }
        }
        return null;
    }

    @Test
    public void testHasGaugeTag() throws Exception {
        DynamicGauge.set("test1", tagList, 0);
        DoubleGauge c = getByName("test1");
        Tag type = c.getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "GAUGE");
    }

    /**
     * Erase all previous gauges by creating a new loading cache with a short expiration time
     */
    @BeforeMethod
    public void setupInstance() throws Exception {
        LOGGER.info("Setting up DynamicGauge instance with a new cache");
        DynamicGauge theInstance = getInstance();
        Field gauges = DynamicGauge.class.getDeclaredField("gauges");
        gauges.setAccessible(true);
        LoadingCache<MonitorConfig, DoubleGauge> newShortExpiringCache = CacheBuilder.newBuilder()
                .expireAfterAccess(1, TimeUnit.SECONDS)
                .build(new CacheLoader<MonitorConfig, DoubleGauge>() {
                    @Override
                    public DoubleGauge load(final MonitorConfig config) throws Exception {
                        return new DoubleGauge(config);
                    }
                });

        gauges.set(theInstance, newShortExpiringCache);
    }

    @Test
    public void testGetValue() throws Exception {
        DynamicGauge.set("test1", tagList, 1);
        DoubleGauge c = getByName("test1");
        assertEquals(c.getValue(), 1.0);
        c.set(13.0);
        assertEquals(c.getValue(), 13.0);
    }

    @Test
    public void testExpiration() throws Exception {
        DynamicGauge.set("test1", tagList, 0);
        DynamicGauge.set("test2", tagList, 0);

        Thread.sleep(500L);
        DynamicGauge.set("test1", tagList, 2);

        Thread.sleep(500L);
        DynamicGauge.set("test1", tagList, 4);

        Thread.sleep(200L);
        DoubleGauge c1 = getByName("test1");
        assertEquals(c1.getValue(), 4.0);

        DoubleGauge c2 = getByName("test2");
        assertNull(c2, "Gauges not used in a while should expire");
    }

    @Test
    public void testByStrings() throws Exception {
        DynamicGauge.set("byName", 0);
        DynamicGauge.set("byName", 3);

        DoubleGauge c = getByName("byName");
        assertEquals(c.getValue(), 3.0);

        DynamicGauge.set(MonitorConfig.of("byName2", "key", "value", "key2", "value2"), 0);
        DynamicGauge.set(MonitorConfig.of("byName2", "key", "value", "key2", "value2"), 3);
        DoubleGauge c2 = getByName("byName2");
        assertEquals(c2.getValue(), 3.0);
    }
}
