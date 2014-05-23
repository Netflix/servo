package com.netflix.servo.jmx;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.testng.annotations.Test;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.BasicCounter;
import com.netflix.servo.monitor.MonitorConfig;

import static org.testng.Assert.*;

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
