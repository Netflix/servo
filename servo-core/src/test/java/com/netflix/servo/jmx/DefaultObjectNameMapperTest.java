/**
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
package com.netflix.servo.jmx;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.MonitorConfig;
import org.testng.annotations.Test;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import static org.testng.Assert.assertEquals;

public class DefaultObjectNameMapperTest {

    private static final ObjectNameMapper DEFAULT_MAPPER = new DefaultObjectNameMapper();
    private static final String TEST_DOMAIN = "testDomain";

    @Test
    public void testStandardMapping() {
        MonitorConfig config = MonitorConfig.builder("testName").withTag("foo", "bar").build();
        ObjectName name = DEFAULT_MAPPER.createObjectName(TEST_DOMAIN, new BasicCounter(config));
        assertEquals(name.getDomain(), TEST_DOMAIN);
        assertEquals(name.getKeyPropertyListString(),
                String.format("name=testName,%s=COUNTER,foo=bar",
                        DataSourceType.KEY));
    }

    @Test
    public void testMultipleTags() throws MalformedObjectNameException {
        BasicCounter counter = new BasicCounter(
                MonitorConfig.builder("testName")
                        .withTag("bbb", "foo")
                        .withTag("aaa", "bar")
                        .withTag("zzz", "test")
                        .build());
        ObjectName name = DEFAULT_MAPPER.createObjectName(TEST_DOMAIN, counter);
        assertEquals(name.getDomain(), TEST_DOMAIN);
        assertEquals(name.getKeyPropertyListString(),
                String.format("name=testName,aaa=bar,bbb=foo,zzz=test,%s=COUNTER",
                        DataSourceType.KEY));
    }

}
