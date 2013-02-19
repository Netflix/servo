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

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class BasicInformationalTest extends AbstractMonitorTest<BasicInformational> {

    public BasicInformational newInstance(String name) {
        return new BasicInformational(MonitorConfig.builder(name).build());
    }

    @Test
    public void testGetValue() throws Exception {
        BasicInformational c = newInstance("foo");
        assertEquals(c.getValue(), null);
        c.setValue("bar");
        assertEquals(c.getValue(), "bar");
    }

    @Test
    public void testEqualsSet() throws Exception {
        BasicInformational c1 = newInstance("foo");
        BasicInformational c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.setValue("bar");
        assertNotEquals(c1, c2);
        c2.setValue("bar");
        assertEquals(c1, c2);
    }
}
