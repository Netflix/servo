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

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableSet;

import com.netflix.servo.MonitorRegistry;

import com.netflix.servo.annotations.AnnotatedObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple monitor registry backed by a {@link java.util.Set}.
 */
public final class BasicMonitorRegistry implements MonitorRegistry {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<AnnotatedObject> objects;

    /**
     * Creates a new instance.
     */
    public BasicMonitorRegistry() {
        objects = Collections.synchronizedSet(new HashSet<AnnotatedObject>());
    }

    /** {@inheritDoc} */
    public void registerObject(Object obj) {
        Preconditions.checkNotNull(obj, "obj cannot be null");
        try {
            objects.add(new AnnotatedObject(obj));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    /** {@inheritDoc} */
    public void unRegisterObject(Object obj) {
        Preconditions.checkNotNull(obj, "obj cannot be null");
        try {
            objects.remove(new AnnotatedObject(obj));
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid object", e);
        }
    }

    /** {@inheritDoc} */
    public Set<AnnotatedObject> getRegisteredObjects() {
        return ImmutableSet.copyOf(objects);
    }
}
