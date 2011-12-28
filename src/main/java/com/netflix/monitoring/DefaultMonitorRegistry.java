/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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
package com.netflix.monitoring;

import java.util.Properties;

public class DefaultMonitorRegistry implements MonitorRegistry {

    private static final String REGISTRY_CLASS_PROP =
        "com.netflix.monitoring.registryClass";

    private static MonitorRegistry INSTANCE = new DefaultMonitorRegistry();

    private final MonitorRegistry mRegistry;

    private static MonitorRegistry getInstance() {
        return INSTANCE;
    }

    DefaultMonitorRegistry() {
        this(System.getProperties());
    }

    DefaultMonitorRegistry(Properties props) {
        String className = props.getProperty(REGISTRY_CLASS_PROP);
        if (className != null) {
            try {
                Class<?> c = Class.forName(className);
                mRegistry = (MonitorRegistry) c.newInstance();
            } catch (Throwable t) {
                throw new IllegalArgumentException(
                    "failed to create instance of class " + className, t);
            }
        } else {
            mRegistry = new JmxMonitorRegistry();
        }
    }

    public void registerObject(Object obj) {
        mRegistry.registerObject(obj);
    }

    public void unRegisterObject(Object obj) {
        mRegistry.unRegisterObject(obj);
    }
}
