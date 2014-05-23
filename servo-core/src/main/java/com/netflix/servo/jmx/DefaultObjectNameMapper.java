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

import javax.management.ObjectName;

import com.netflix.servo.monitor.Monitor;

/**
 * The default {@link ObjectNameMapper} implementation that
 * is used by the {@link JmxMonitorRegistry}. This implementation
 * simply appends the monitor's name followed by the tags for the monitor.
 *
 */
class DefaultObjectNameMapper implements ObjectNameMapper {

    @Override
    public ObjectName createObjectName(String domain, Monitor<?> monitor) {
        ObjectNameBuilder objNameBuilder = ObjectNameBuilder.forDomain(domain);
        objNameBuilder.addProperty("name", monitor.getConfig().getName());
        objNameBuilder.addProperties(monitor.getConfig().getTags());
        return objNameBuilder.build();
    }

}
