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
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;

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
     * Return the value of the field/method annotated with
     * {@link MonitorTags}.
     */
    public static TagList getMonitorTags(Object obj) throws Exception {
        List<AccessibleObject> fields =
            getAnnotatedFields(MonitorTags.class, obj, 1);
        return fields.isEmpty()
            ? BasicTagList.EMPTY
            : (TagList) getValue(obj, fields.get(0));
    }

    /** Return the list of fields/methods annotated with {@link Monitor}. */
    public static List<AnnotatedAttribute> getMonitoredAttributes(Object obj) {
        List<AccessibleObject> fields =
            getAnnotatedFields(Monitor.class, obj, Integer.MAX_VALUE);
        ImmutableList.Builder<AnnotatedAttribute> attrs =
            ImmutableList.builder();
        for (AccessibleObject field : fields) {
            Monitor m = field.getAnnotation(Monitor.class);
            attrs.add(new AnnotatedAttribute(obj, m, field));
        }
        return attrs.build();
    }

    /** Check that the object conforms to annotation requirements. */
    public static void validate(Object obj) {
        try {
            getMonitorTags(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "invalid MonitorTags annotation on object " + obj, e);
        }

        List<AnnotatedAttribute> attrs = getMonitoredAttributes(obj);
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException(
                "no Monitor annotations on object " + obj);
        }
        String ctype = obj.getClass().getCanonicalName();
        for (AnnotatedAttribute attr : attrs) {
            Monitor m = attr.getAnnotation();
            Object value = null;
            try {
                value = attr.getValue();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                    "failed to get value for " + m + " on " + ctype, e);
            }

            if (m.type() != DataSourceType.INFORMATIONAL) {
                String vtype = (value == null)
                    ? null
                    : value.getClass().getCanonicalName();
                Number n = asNumber(value);
                if (n == null) {
                    throw new IllegalArgumentException(
                        "expected java.lang.Number, but received " + vtype +
                        " for " + m + " on " + ctype);
                }
            }
        }
    }

    /**
     * Try to convert an object into a number. Boolean values will return 1 if
     * true and 0 if false. If the value is null or an unknown data type null
     * will be returned.
     */
    public static Number asNumber(Object value) {
        Number num = null;
        if (value == null) {
            num = null;
        } else if (value instanceof Number) {
            num = (Number) value;
        } else if (value instanceof Boolean) {
            num = ((Boolean) value) ? 1 : 0;
        }
        return num;
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
     * Get the value of a field or accessor method of {@code obj} identified
     * by {@code attr} as a number. See {@link #asNumber} for details on the
     * conversion.
     *
     * @param obj   the instance to query
     * @param attr  the field or method to retrieve
     * @return      value of the field or method
     */
    public static Number getNumber(Object obj, AccessibleObject attr)
            throws Exception {
        return asNumber(getValue(obj, attr));
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
    private static List<AccessibleObject> getAnnotatedFields(
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
