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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AnnotationUtilsTest {

    public static class FieldIdObject {
        @MonitorId
        private final String id;

        public FieldIdObject(String id) {
            this.id = id;
        }
    }

    public static class MethodIdObject {
        private final String id;

        public MethodIdObject(String id) {
            this.id = id;
        }

        @MonitorId
        private String getId() {
            return id;
        }
    }

    public static class TwoIdObject {
        @MonitorId
        private final String id;

        public TwoIdObject(String id) {
            this.id = id;
        }

        @MonitorId
        private String getId() {
            return id;
        }
    }

    public static class IntIdObject {
        @MonitorId
        private final int id;

        public IntIdObject(int id) {
            this.id = id;
        }
    }

    public static class MethodParamsIdObject {
        private final String id;

        public MethodParamsIdObject(String id) {
            this.id = id;
        }

        @MonitorId
        private String getId(int p) {
            return id;
        }
    }

    @Test
    public void testGetMonitorIdFromField() throws Exception {
        Object obj = new FieldIdObject("foo");
        String id = AnnotationUtils.getMonitorId(obj);
        assertEquals(id, "foo");
    }

    @Test
    public void testGetMonitorIdFromMethod() throws Exception {
        Object obj = new MethodIdObject("foo");
        String id = AnnotationUtils.getMonitorId(obj);
        assertEquals(id, "foo");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetMonitorIdWithTwo() throws Exception {
        Object obj = new TwoIdObject("foo");
        AnnotationUtils.getMonitorId(obj);
    }

    @Test(expectedExceptions = ClassCastException.class)
    public void testGetMonitorIdWithInt() throws Exception {
        Object obj = new IntIdObject(42);
        AnnotationUtils.getMonitorId(obj);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testGetMonitorIdWithMethodParams() throws Exception {
        Object obj = new MethodParamsIdObject("foo");
        AnnotationUtils.getMonitorId(obj);
    }
}
