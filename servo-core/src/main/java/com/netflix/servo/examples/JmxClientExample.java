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
package com.netflix.servo.examples;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * JMX client that grabs the MonitoredResources.
 */
public final class JmxClientExample {

    private JmxClientExample() {
    }

    private static final int INDENT_SPACES = 4;

    private static JMXServiceURL mkJmxUrl(String host, String port)
            throws MalformedURLException {
        String url = String.format(
            "service:jmx:rmi://%1$s:%2$s/jndi/rmi://%1$s:%2$s/jmxrmi",
            host, port);
        return new JMXServiceURL(url);
    }

    private static void println(int indent, String msg) {
        for (int i = 0; i < indent * INDENT_SPACES; ++i) {
            System.out.print(" ");
        }
        System.out.println(msg);
    }

    private static void dumpValue(int indent, String name, Object obj) {
        if (obj instanceof CompositeData) {
            CompositeData cd = (CompositeData) obj;
            println(indent, String.format("%s:", name));
            for (String key : cd.getCompositeType().keySet()) {
                dumpValue(indent + 1, key, cd.get(key));
            }
        } else {
            println(indent, String.format("%s => %s", name, obj));
        }
    }

    private static void dumpObj(MBeanServerConnection con, ObjectName objName)
            throws Exception {
        System.out.println("ObjectName: " + objName.toString());

        MBeanAttributeInfo[] attrs = con.getMBeanInfo(objName).getAttributes();
        if (attrs != null) {
            String[] attrNames = new String[attrs.length];
            for (int i = 0; i < attrNames.length; ++i) {
                attrNames[i] = attrs[i].getName();
            }

            AttributeList list = con.getAttributes(objName, attrNames);
            for (Attribute a : list.asList()) {
                dumpValue(1, a.getName(), a.getValue());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: JMXClientExample <host> <port>");
            System.exit(1);
        }
        String host = args[0];
        String port = args[1];

        JMXServiceURL url = mkJmxUrl(host, port);

        MBeanServerConnection con = JMXConnectorFactory.connect(
            url, null).getMBeanServerConnection();

        ObjectName pattern = new ObjectName(
            "com.netflix.servo.jmx.MonitoredResource:*");

        Set<ObjectName> objNames = con.queryNames(pattern, null);
        for (ObjectName objName : objNames) {
            dumpObj(con, objName);
        }
    }
}
