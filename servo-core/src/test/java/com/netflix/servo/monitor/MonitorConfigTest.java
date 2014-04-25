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

import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MonitorConfigTest {

    private final TagList tags1 = new BasicTagList(SortedTagList.builder().withTag("cluster", "foo")
            .withTag("asg", "foo-v000").build());

    private final TagList tags2 = new BasicTagList(SortedTagList.builder().withTag("cluster", "foo")
            .withTag("asg", "foo-v001").build());

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullName() throws Exception {
        new MonitorConfig.Builder((String) null).withTags(tags1).build();
    }

    @Test
    public void testNullTags() throws Exception {
        MonitorConfig m = new MonitorConfig.Builder("a").build();
        assertEquals(m, new MonitorConfig.Builder("a").build());
    }

    @Test
    public void testAccessors() throws Exception {
        MonitorConfig m1 = new MonitorConfig.Builder("a").withTags(tags1).build();
        assertEquals(m1.getName(), "a");
        assertEquals(m1.getTags(), tags1);
    }

    @Test
    public void testEquals() throws Exception {
        MonitorConfig m1 = new MonitorConfig.Builder("a").withTags(tags1).build();
        MonitorConfig m2 = new MonitorConfig.Builder("a").withTags(tags2).build();
        MonitorConfig m3 = new MonitorConfig.Builder("a").withTags(tags1).build();

        assertNotNull(m1);
        assertFalse(m1.toString().equals(m2.toString()));
        assertTrue(m1.equals(m1));
        assertFalse(m1.equals(m2));
        assertTrue(m1.equals(m3));
    }

    @Test
    public void testHashCode() throws Exception {
        MonitorConfig m1 = new MonitorConfig.Builder("a").withTags(tags1).build();
        MonitorConfig m2 = new MonitorConfig.Builder("a").withTags(tags2).build();
        MonitorConfig m3 = new MonitorConfig.Builder("a").withTags(tags1).build();

        assertTrue(m1.hashCode() == m1.hashCode());
        assertTrue(m1.hashCode() != m2.hashCode());
        assertTrue(m1.hashCode() == m3.hashCode());
    }

    @Test
    public void testBuilderMonitorConfig() throws Exception {
        MonitorConfig m1 = new MonitorConfig.Builder("a").build();
        MonitorConfig m2 = new MonitorConfig.Builder(m1).build();
        assertEquals(m1, m2);

        MonitorConfig m3 = new MonitorConfig.Builder(m1).withTag("k", "v").build();
        assertNotEquals(m1, m3);
        assertEquals(m1.getName(), m3.getName());
        assertNotEquals(m1.getTags(), m3.getTags());
        assertEquals(m1.getPublishingPolicy(), m3.getPublishingPolicy());
    }
}
