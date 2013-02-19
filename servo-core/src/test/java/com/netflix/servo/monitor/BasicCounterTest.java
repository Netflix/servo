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

import com.netflix.servo.tag.Tag;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BasicCounterTest extends AbstractMonitorTest<BasicCounter> {

    public BasicCounter newInstance(String name) {
        return new BasicCounter(MonitorConfig.builder(name).build());
    }

    @Test
    public void testHasCounterTag() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "COUNTER");
    }

    @Test
    public void testGetValue() throws Exception {
        BasicCounter c = newInstance("foo");
        assertEquals(c.getValue().longValue(), 0L);
        c.increment();
        assertEquals(c.getValue().longValue(), 1L);
        c.increment(13);
        assertEquals(c.getValue().longValue(), 14L);
    }

    @Test
    public void testEqualsCount() throws Exception {
        BasicCounter c1 = newInstance("foo");
        BasicCounter c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.increment();
        assertNotEquals(c1, c2);
        c2.increment();
        assertEquals(c1, c2);
    }
}
