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
package com.netflix.servo.publish;

import javax.management.MBeanServerConnection;
import java.lang.management.ManagementFactory;

/**
 * Retrieves a connection to the local mbean server running in the same JVM.
 */
public final class LocalJmxConnector implements JmxConnector {

    /** Creates a new instance. */
    public LocalJmxConnector() {
    }

    /** {@inheritDoc} */
    public MBeanServerConnection getConnection() {
        return ManagementFactory.getPlatformMBeanServer();
    }
}
