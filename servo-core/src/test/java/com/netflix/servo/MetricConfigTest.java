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
package com.netflix.servo;

import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class MetricConfigTest {

    private final TagList tags1 =
        BasicTagList.copyOf("cluster=foo", "asg=foo-v000");

    private final TagList tags2 =
        BasicTagList.copyOf("cluster=foo", "asg=foo-v001");

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullName() throws Exception {
        new MetricConfig(null, tags1);
    }

    @Test
    public void testNullTags() throws Exception {
        MetricConfig m = new MetricConfig("a");
        assertEquals(m, new MetricConfig("a"));
    }

    @Test
    public void testAccessors() throws Exception {
        MetricConfig m1 = new MetricConfig("a", tags1);
        assertEquals(m1.getName(), "a");
        assertEquals(m1.getTags(), tags1);
    }

    @Test
    public void testEquals() throws Exception {
        MetricConfig m1 = new MetricConfig("a", tags1);
        MetricConfig m2 = new MetricConfig("a", tags2);
        MetricConfig m3 = new MetricConfig("a", tags1);

        assertFalse(m1.equals(null));
        assertFalse(m1.equals(m2.toString()));
        assertTrue(m1.equals(m1));
        assertFalse(m1.equals(m2));
        assertTrue(m1.equals(m3));
    }

    @Test
    public void testHashCode() throws Exception {
        MetricConfig m1 = new MetricConfig("a", tags1);
        MetricConfig m2 = new MetricConfig("a", tags2);
        MetricConfig m3 = new MetricConfig("a", tags1);

        assertTrue(m1.hashCode() == m1.hashCode());
        assertTrue(m1.hashCode() != m2.hashCode());
        assertTrue(m1.hashCode() == m3.hashCode());
    }
}
