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
package com.netflix.servo.annotations;

import com.google.common.base.Preconditions;
import com.google.common.base.Objects;

import com.google.common.collect.ImmutableList;

import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;
import com.netflix.servo.jmx.MonitoredAttribute;

import java.lang.annotation.Annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;

/**
 * Helper functions for querying the monitor annotations associated with a
 * class.
 */
public final class AnnotatedObject {

    private final Object object;
    private final String id;
    private final TagList tags;
    private final List<AnnotatedAttribute> attrs;

    public AnnotatedObject(Object obj) throws Exception {
        object = Preconditions.checkNotNull(obj);
        id = AnnotationUtils.getMonitorId(obj);
        tags = AnnotationUtils.getMonitorTags(obj);
        attrs = AnnotationUtils.getMonitoredAttributes(obj);
    }

    public Object getObject() {
        return object;
    }

    public String getId() {
        return id;
    }

    public TagList getTags() {
        return tags;
    }

    public List<AnnotatedAttribute> getAttributes() {
        return attrs;
    }

    public String getClassName() {
        return object.getClass().getCanonicalName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof AnnotatedObject)) {
            return false;
        }
        AnnotatedObject annoObj = (AnnotatedObject) obj;
        return object == annoObj.getObject();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }
}
