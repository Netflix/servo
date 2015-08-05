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

import com.netflix.servo.monitor.Monitor;

import javax.management.ObjectName;

/**
 * Allows for different implementations when mapping a
 * monitor to a JMX {@link ObjectName}. The mapper can be
 * can be specified when using the {@link JmxMonitorRegistry}.
 * This interface also has a reference to the default mapping implementation.
 * Note that an {@link ObjectName}'s properties are meant to be unordered,
 * however, some tools such as VisualVM use the order to build a hierarchy
 * view where the default implementation may not be desirable.
 */
public interface ObjectNameMapper {

  /**
   * The default mapping implementation. This implementation simply
   * appends the monitor's name followed by all the tags as properties
   * of the {@link ObjectName}. The mapper remaps any characters that are
   * not alphanumeric, a period, or hypen to an underscore.
   */
  ObjectNameMapper DEFAULT = new DefaultObjectNameMapper();

  /**
   * Given the domain and monitor generates an {@link ObjectName} to use.
   *
   * @param domain  the JMX domain
   * @param monitor the monitor
   * @return The created ObjectName
   */
  ObjectName createObjectName(String domain, Monitor<?> monitor);

}
