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
package com.netflix.servo;

import com.netflix.servo.annotations.AnnotatedObject;

import java.util.Set;

/**
 * Registry to keep track of objects with
 * {@link com.netflix.servo.annotations.Monitor} annotations.
 */
public interface MonitorRegistry {
    /**
     * Register the object so annotated fields can be made available for
     * querying.
     */
    void registerAnnotatedObject(Object obj);

    /**
     * Un-register the object. It should be assumed that the registry will
     * hold a reference to the object, and thus preventing garbage collection,
     * unless it is unregistered.
     */
    void unregisterAnotatedObject(Object obj);

    /**
     * Returns a set of all registered objects.
     */
    Set<AnnotatedObject> getRegisteredAnnotatedObjects();

    /**
     * The set of registered Monitor objects.
     * @return
     */
    Set<Monitor> getRegisteredMonitors();

    /**
     * Register a new monitor in the registry.
     * @param monitor
     */
    void register(Monitor monitor);

    /**
     * Unregister a Monitor from the registry.
     * @param monitor
     */
    void unregister(Monitor monitor);
}
