package com.netflix.servo.jmx;

import com.netflix.servo.Monitor;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.monitor.BasicCounter;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;
import static org.testng.Assert.assertEquals;

import javax.management.DynamicMBean;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

/**
 * User: gorzell
 * Date: 5/9/12
 * Time: 10:58 AM
 */
public class MonitorModelMBeanTest {
    Monitor testMonitor;
    String testObjectNameWithPrefix;
    String testObjectName;

    @BeforeTest
    public void setUp() throws Exception {
        testMonitor = new BasicCounter(new MonitorContext.Builder("testCounter").withTag("tag", "test").build());
        testObjectName = "com.netflix.servo:name=testCounter,tag=test";
        testObjectNameWithPrefix = "test:name=testCounter,tag=test";
    }

    @Test
    public void testCreateObjectName() throws Exception{
        ObjectName name = MonitorModelMBean.createObjectName("test", testMonitor.getContext());
        assertEquals(testObjectNameWithPrefix, name.toString());

        name = MonitorModelMBean.createObjectName(null, testMonitor.getContext());
        assertEquals(testObjectName, name.toString());
    }

    @Test (expectedExceptions = {NullPointerException.class})
    public void testCreateObjectNameNull() throws Exception {
        MonitorModelMBean.createObjectName(null,null);
    }

    @Test
    public void testNewInstance() throws Exception {
        MonitorModelMBean bean = MonitorModelMBean.newInstance("test", testMonitor);
        assertEquals(testObjectNameWithPrefix, bean.getObjectName().toString());

        ModelMBean modelMBean = bean.getMBean();
        //For now we should have one operation for each attribute and no more or less
        assertEquals(modelMBean.getMBeanInfo().getAttributes().length, modelMBean.getMBeanInfo().getOperations().length);

        assertEquals(testMonitor.getContext().getName(),
                modelMBean.getMBeanInfo().getDescriptor().getFieldValue("displayName"));
    }

    @Test
    public void testNewInstanceNullName() throws Exception{
        MonitorModelMBean bean = MonitorModelMBean.newInstance(null, testMonitor);
        assertEquals(testObjectName, bean.getObjectName().toString());
    }

    @Test (expectedExceptions = {java.lang.IllegalArgumentException.class})
    public void testNewInstanceNullMonitor() throws Exception{
        MonitorModelMBean.newInstance("test", null);
    }
}
