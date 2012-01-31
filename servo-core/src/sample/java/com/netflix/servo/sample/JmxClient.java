/*
 * #%L
 * servo-core
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
package com.netflix.servo.sample;

import javax.management.AttributeList;
import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Set;

/**
 * User: gorzell
 * Date: 1/10/12
 * Time: 10:03 AM
 */
public class JmxClient {
    public static void main(String[] args) {

        try {
            if (args.length < 2) {
                System.out.println("Usage: java JMXClien <host> <port>");
                System.exit(1);
            }
            String host = args[0];
            String port = args[1];

            System.out.println("All currently registered MonitoredResources:");

            String jmxURL = "service:jmx:rmi://" + host + ":" + port + "/jndi/rmi://" + host + ":" + port + "/jmxrmi";

            MBeanServerConnection connection = JMXConnectorFactory.connect(new JMXServiceURL(jmxURL),
                    null).getMBeanServerConnection();

            ObjectName pattern = new ObjectName("com.netflix.MonitoredResources:*");
            Set<ObjectName> objNames = connection.queryNames(pattern, null);

            for (ObjectName objName : objNames) {

                System.out.println("\tObjectName: " + objName.toString());

                MBeanAttributeInfo[] attrs = connection.getMBeanInfo(objName).getAttributes();
                if (attrs != null) {
                    String attrStrings[] = new String[attrs.length];
                    for (int i = 0; i < attrStrings.length; i++) {
                        attrStrings[i] = attrs[i].getName();
                    }
                    AttributeList list = connection.getAttributes(objName, attrStrings);

                    for (Attribute a : list.asList()) {
                        System.out.println("\t\t" + a.getName() + ": ");
                        if (a.getValue() instanceof CompositeDataSupport) {
                            CompositeDataSupport compositeData = (CompositeDataSupport) a.getValue();
                            CompositeType compositeType = compositeData.getCompositeType();
                            for (Object key : compositeType.keySet()) {
                                System.out.println("\t\t\t" + key + " => " + compositeData.get((String) key));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
