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
package com.netflix.servo.jmx;

import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.BasicCounter;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

public class MonitorModelMBeanTest {
    Monitor testMonitor;
    String testObjectNameWithPrefix;
    String testObjectName;

    @BeforeTest
    public void setUp() throws Exception {
        testMonitor = new BasicCounter(new MonitorConfig.Builder("testCounter").withTag("tag", "test").build());
        testObjectName = "com.netflix.servo:name=testCounter,tag=test,type=COUNTER";
        testObjectNameWithPrefix = "test:name=testCounter,tag=test,type=COUNTER";
    }

    @Test
    public void testCreateObjectName() throws Exception{
        ObjectName name = MonitorModelMBean.createObjectName("test", testMonitor.getConfig());
        assertEquals(testObjectNameWithPrefix, name.toString());

        name = MonitorModelMBean.createObjectName(null, testMonitor.getConfig());
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

        assertEquals(testMonitor.getConfig().getName(),
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
