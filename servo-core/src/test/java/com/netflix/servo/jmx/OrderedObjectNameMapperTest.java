/**
 * Copyright 2014 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.jmx;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import org.testng.annotations.Test;

import javax.management.ObjectName;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;

public class OrderedObjectNameMapperTest {

  private static final String TEST_DOMAIN = "testDomain";
  private static final Counter TEST_COUNTER =
      new BasicCounter(
          MonitorConfig.builder("testName")
              .withTag("zzz", "zzzVal")
              .withTag("foo", "bar")
              .withTag("aaa", "aaaVal")
              .build());

  @Test
  public void testOrderedTagsWithAppend() {
    ObjectNameMapper mapper =
        new OrderedObjectNameMapper(true, "name",
            DataSourceType.KEY, "foo", "notPresentKey");
    ObjectName name = mapper.createObjectName(TEST_DOMAIN, TEST_COUNTER);
    assertEquals(name.getDomain(), TEST_DOMAIN);
    assertEquals(name.getKeyPropertyListString(),
        String.format("name=testName,%s=COUNTER,foo=bar,aaa=aaaVal,zzz=zzzVal",
            DataSourceType.KEY));
  }

  @Test
  public void testOrderedTagsWithoutAppend() {
    ObjectNameMapper mapper = new OrderedObjectNameMapper(false,
        Arrays.asList("name", DataSourceType.KEY, "foo", "notPresentKey"));
    ObjectName name = mapper.createObjectName(TEST_DOMAIN, TEST_COUNTER);
    assertEquals(name.getDomain(), TEST_DOMAIN);
    assertEquals(name.getKeyPropertyListString(),
        String.format("name=testName,%s=COUNTER,foo=bar",
            DataSourceType.KEY));
  }

  @Test
  public void testOrderedTagsWithoutNameExplicitlyOrdered() {
    ObjectNameMapper mapper = new OrderedObjectNameMapper(true, "foo", DataSourceType.KEY);
    ObjectName name = mapper.createObjectName(TEST_DOMAIN, TEST_COUNTER);
    assertEquals(name.getDomain(), TEST_DOMAIN);
    assertEquals(name.getKeyPropertyListString(),
        String.format("foo=bar,%s=COUNTER,name=testName,aaa=aaaVal,zzz=zzzVal",
            DataSourceType.KEY));
  }

}
