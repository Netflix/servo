package com.netflix.servo.jmx;

import javax.management.ObjectName;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;

import static org.testng.Assert.*;

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
                Lists.newArrayList("name", DataSourceType.KEY, "foo", "notPresentKey"));
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
