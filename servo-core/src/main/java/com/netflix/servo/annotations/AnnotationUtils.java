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
public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    /**
     * Return the value of the field/method annotated with {@link MonitorId}.
     */
    public static String getMonitorId(Object obj) throws Exception {
        List<AccessibleObject> attrs =
            getAnnotatedAttributes(MonitorId.class, obj, 1);
        return attrs.isEmpty() ? null : (String) getValue(obj, attrs.get(0));
    }

    /**
     * Return the value of the field/method annotated with
     * {@link MonitorTags}.
     */
    public static TagList getMonitorTags(Object obj) throws Exception {
        List<AccessibleObject> attrs =
            getAnnotatedAttributes(MonitorTags.class, obj, 1);
        return attrs.isEmpty()
            ? BasicTagList.EMPTY
            : (TagList) getValue(obj, attrs.get(0));
    }

    /** Return the list of fields/methods annotated with {@link Monitor}. */
    public static List<MonitoredAttribute> getMonitoredAttributes(Object obj) {
        List<AccessibleObject> annotatedAttrs =
            getAnnotatedAttributes(Monitor.class, obj, Integer.MAX_VALUE);
        ImmutableList.Builder<MonitoredAttribute> monitoredAttrs =
            ImmutableList.builder();
        for (AccessibleObject attr : annotatedAttrs) {
            Monitor m = attr.getAnnotation(Monitor.class);
            monitoredAttrs.add(new MonitoredAttribute(m, attr, obj));
        }
        return monitoredAttrs.build();
    }

    /** Check that the object conforms to annotation requirements. */
    public static void validate(Object obj) {

    }

    /**
     * Get the value of a field or accessor method of {@code obj} identified
     * by {@code attr}.
     *
     * @param obj   the instance to query
     * @param attr  the field or method to retrieve
     * @return      value of the field or method
     */
    public static Object getValue(Object obj, AccessibleObject attr)
            throws Exception {
        return (attr instanceof Field)
            ? ((Field) attr).get(obj)
            : ((Method) attr).invoke(obj);
    }

    /**
     * Helper to return all fields or methods that have the specified
     * annotation.
     *
     * @param annotationClass  the type of annotation to check for
     * @param obj              instance to query
     * @param maxPerClass      max number of annotated attributes that are
     *                         permitted for this class
     * @return                 list of matching attributes
     */
    private static List<AccessibleObject> getAnnotatedAttributes(
            Class<? extends Annotation> annotationClass,
            Object obj,
            int maxPerClass) {
        ImmutableList.Builder<AccessibleObject> attrs = ImmutableList.builder();

        // Fields
        Class<?> objClass = obj.getClass();
        for (Field field : objClass.getDeclaredFields()) {
            Object annotation = field.getAnnotation(annotationClass);
            if (annotation != null) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                attrs.add(field);
            }
        }

        // Methods
        for (Method method : objClass.getDeclaredMethods()) {
            Object annotation = method.getAnnotation(annotationClass);
            if (annotation != null) {
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                attrs.add(method);
            }
        }

        // Verify limit
        List<AccessibleObject> attrList = attrs.build();
        if (attrList.size() > maxPerClass) {
            throw new IllegalArgumentException(String.format(
                "class %s has %d attributes annotated with %s",
                obj.getClass().getCanonicalName(),
                attrList.size(),
                annotationClass.getCanonicalName()));
        }
        return attrList;
    }
}
