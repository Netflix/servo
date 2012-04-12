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
package com.netflix.servo.annotations;

import com.google.common.collect.Maps;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

public class AnnotationUtilsTest {

    public static class StringArrayTagObject {
        @MonitorTags
        private final String[] tags;

        public StringArrayTagObject(String... tags) {
            this.tags = tags;
        }
    }

    public static class FieldTagObject {
        @MonitorTags
        private final TagList tags;

        public FieldTagObject(String... tags) {
            this.tags = BasicTagList.copyOf(tags);
        }
    }

    public static class MethodTagObject {
        private final TagList tags;

        public MethodTagObject(String... tags) {
            this.tags = BasicTagList.copyOf(tags);
        }

        @MonitorTags
        private TagList getTags() {
            return tags;
        }
    }

    public static class MonitorObject {

        @Monitor(name="zero", type=DataSourceType.GAUGE)
        boolean zero = false;

        @Monitor(name="one", type=DataSourceType.GAUGE)
        boolean one = true;

        private float two = 2.0f;

        @Monitor(name="three", type=DataSourceType.GAUGE)
        byte three = (byte) 3;

        @Monitor(name="four", description="useful information")
        public int four = 4;

        @Monitor(name="five", type=DataSourceType.COUNTER)
        private long five = 5L;

        @Monitor(name="six")
        private double six = 6.0;

        public MonitorObject() {
            
        }

        @Monitor(name="two")
        public float getTwo() {
            return two;
        }
    }

    public static class StringGaugeObject {

        @Monitor(name="foo", type=DataSourceType.GAUGE)
        String foo = "bar";

        public StringGaugeObject() {
        }
    }

    public static class StringCounterObject {

        @Monitor(name="foo", type=DataSourceType.COUNTER)
        String foo = "bar";

        public StringCounterObject() {
        }
    }

    @Test
    public void testGetMonitorTagsFromField() throws Exception {
        Object obj = new FieldTagObject("foo=bar");
        TagList tags = AnnotationUtils.getMonitorTags(obj);
        assertEquals(tags, BasicTagList.copyOf("foo=bar"));
    }

    @Test
    public void testGetMonitorTagsFromMethod() throws Exception {
        Object obj = new MethodTagObject("foo=bar");
        TagList tags = AnnotationUtils.getMonitorTags(obj);
        assertEquals(tags, BasicTagList.copyOf("foo=bar"));
    }

    @Test
    public void testGetMonitorTagsWithNoTags() throws Exception {
        Object obj = new Object();
        assertEquals(AnnotationUtils.getMonitorTags(obj), BasicTagList.EMPTY);
    }

    @Test
    public void testGetMonitoredAttributes() throws Exception {
        MonitorObject obj = new MonitorObject();
        List<AnnotatedAttribute> attrList =
            AnnotationUtils.getMonitoredAttributes(obj);
        assertEquals(attrList.size(), 7);

        Map<String,AnnotatedAttribute> attrs = Maps.newHashMap();
        for (AnnotatedAttribute a : attrList) {
            attrs.put(a.getAnnotation().name(), a);
        }

        AnnotatedAttribute a = attrs.get("zero");
        assertEquals(a.getValue(), false);
        assertEquals(a.getNumber(), 0);

        a = attrs.get("one");
        assertEquals(a.getValue(), true);
        assertEquals(a.getNumber(), 1);

        a = attrs.get("three");
        assertEquals(a.getNumber().intValue(), 3);

        a = attrs.get("four");
        assertEquals(a.getValue(), 4);
        assertEquals(a.getNumber(), 4);

        a = attrs.get("five");
        assertEquals(a.getValue(), 5L);
        assertEquals(a.getNumber(), 5L);

        a = attrs.get("six");
        assertEquals(a.getNumber().intValue(), 6);
    }

    @Test
    public void testGetMonitoredAttributesWithNoMonitor() throws Exception {
        Object obj = new Object();
        List<AnnotatedAttribute> attrs =
            AnnotationUtils.getMonitoredAttributes(obj);
        assertEquals(attrs.size(), 0);
    }

    @Test
    public void testAsNumber() throws Exception {
        assertEquals(AnnotationUtils.asNumber(null), null);
        assertEquals(AnnotationUtils.asNumber(new Object()), null);
        assertEquals(AnnotationUtils.asNumber(4), 4);
        assertEquals(AnnotationUtils.asNumber(false), 0);
        assertEquals(AnnotationUtils.asNumber(true), 1);
        assertEquals(AnnotationUtils.asNumber("foo"), null);
    }

    @Test
    public void testValidateOk() throws Exception {
        AnnotationUtils.validate(new MonitorObject());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateNoMonitor() throws Exception {
        AnnotationUtils.validate(new Object());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateBadGauge() throws Exception {
        AnnotationUtils.validate(new StringGaugeObject());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testValidateBadCounter() throws Exception {
        AnnotationUtils.validate(new StringCounterObject());
    }
}
