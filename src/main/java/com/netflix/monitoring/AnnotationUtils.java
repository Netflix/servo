package com.netflix.monitoring;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.lang.annotation.Annotation;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;
import java.util.Map;

/**
 * Helper functions for querying the monitor annotations associated with a
 * class.
 */
public class AnnotationUtils {
    /** Return the value of the field/method annotated with @MonitorId. */
    public static String getMonitorId(Object obj) throws Exception {
        List<AccessibleObject> attrs =
            getAnnotatedAttributes(MonitorId.class, obj, 1);
        return attrs.isEmpty() ? null : (String) getValue(obj, attrs.get(0));
    }

    /** Return the value of the field/method annotated with @MonitorTags. */
    @SuppressWarnings("unchecked")
    public static Map<String,String> getMonitorTags(Object obj)
            throws Exception {
        List<AccessibleObject> attrs =
            getAnnotatedAttributes(MonitorTags.class, obj, 1);
        return attrs.isEmpty()
            ? ImmutableMap.<String,String>of()
            : ImmutableMap.copyOf((Map<String,String>) getValue(obj, attrs.get(0)));
    }

    /** Return the list of fields/methods annotated with @Monitor. */
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

    static Object getValue(Object obj, AccessibleObject attr) throws Exception {
        return (attr instanceof Field)
            ? ((Field) attr).get(obj)
            : ((Method) attr).invoke(obj);
    }

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
