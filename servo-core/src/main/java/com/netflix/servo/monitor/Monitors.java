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
package com.netflix.servo.monitor;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.cache.Cache;
import com.google.common.collect.Lists;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.MonitorTags;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TaggingContext;
import com.netflix.servo.tag.TagList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Some helper functions for creating monitor objects.
 */
public final class Monitors {

    /** Name used for composite objects that do not have an explicit id. */
    private static final String DEFAULT_ID = "default";

    /** Function to create basic timers. */
    private static class TimerFactory implements Function<MonitorConfig, Timer> {
        private final TimeUnit unit;

        public TimerFactory(TimeUnit unit) {
            this.unit = unit;
        }

        public Timer apply(MonitorConfig config) {
            return new BasicTimer(config, unit);
        }
    }

    /** Function to create basic counters. */
    private static class CounterFactory implements Function<MonitorConfig, Counter> {
        public Counter apply(MonitorConfig config) {
            return new BasicCounter(config);
        }
    }

    private static final CounterFactory COUNTER_FUNCTION = new CounterFactory();

    private Monitors() {
    }

    /**
     * Create a new timer with only the name specified.
     */
    public static Timer newTimer(String name) {
        return newTimer(name, TimeUnit.MILLISECONDS);
    }

    /**
     * Create a new timer with a name and context. The returned timer will maintain separate
     * sub-monitors for each distinct set of tags returned from the context on an update operation.
     */
    public static Timer newTimer(String name, TaggingContext context) {
        return newTimer(name, TimeUnit.MILLISECONDS, context);
    }

    /**
     * Create a new timer with only the name specified.
     */
    public static Timer newTimer(String name, TimeUnit unit) {
        return new BasicTimer(MonitorConfig.builder(name).build(), unit);
    }

    /**
     * Create a new timer with a name and context. The returned timer will maintain separate
     * sub-monitors for each distinct set of tags returned from the context on an update operation.
     */
    public static Timer newTimer(String name, TimeUnit unit, TaggingContext context) {
        final MonitorConfig config = MonitorConfig.builder(name).build();
        return new ContextualTimer(config, context, new TimerFactory(unit));
    }

    /**
     * Create a new counter instance.
     */
    public static Counter newCounter(String name) {
        return new BasicCounter(MonitorConfig.builder(name).build());
    }

    /**
     * Create a new counter with a name and context. The returned counter will maintain separate
     * sub-monitors for each distinct set of tags returned from the context on an update operation.
     */
    public static Counter newCounter(String name, TaggingContext context) {
        final MonitorConfig config = MonitorConfig.builder(name).build();
        return new ContextualCounter(config, context, COUNTER_FUNCTION);
    }

    /**
     * Helper function to easily create a composite for all monitor fields and
     * annotated attributes of a given object.
     */
    public static CompositeMonitor<?> newObjectMonitor(Object obj) {
        return newObjectMonitor(null, obj);
    }

    /**
     * Helper function to easily create a composite for all monitor fields and
     * annotated attributes of a given object.
     *
     * @param id   a unique id associated with this particular instance of the
     *             object. If multiple objects of the same class are registered
     *             they will have the same config and conflict unless the id
     *             values are distinct.
     *
     * @param obj  object to search for monitors on. All fields of type
     *             {@link Monitor} and fields/methods with a
     *             {@link com.netflix.servo.annotations.Monitor} annotation
     *             will be extracted and returned using
     *             {@link CompositeMonitor#getMonitors()}.
     *
     * @return     composite monitor based on the fields of the class
     */
    public static CompositeMonitor<?> newObjectMonitor(String id, Object obj) {
        final TagList tags = getMonitorTags(obj);

        List<Monitor<?>> monitors = Lists.newArrayList();
        addMonitors(monitors, id, tags, obj);

        final Class<?> c = obj.getClass();
        final String objectId = (id == null) ? DEFAULT_ID : id;
        return new BasicCompositeMonitor(newObjectConfig(c, objectId, tags), monitors);
    }

    /**
     * Creates a new monitor for a thread pool with standard metrics for the pool size, queue size,
     * task counts, etc.
     *
     * @param id    id to differentiate metrics for this pool from others.
     * @param pool  thread pool instance to monitor.
     * @return      composite monitor based on stats provided for the pool
     */
    public static CompositeMonitor<?> newThreadPoolMonitor(String id, ThreadPoolExecutor pool) {
        return newObjectMonitor(id, new MonitoredThreadPool(pool));
    }

    /**
     * Creates a new monitor for a cache with standard metrics for the hits, misses, and loads.
     *
     * @param id     id to differentiate metrics for this cache from others.
     * @param cache  cache instance to monitor.
     * @return       composite monitor based on stats provided for the cache
     */
    public static CompositeMonitor<?> newCacheMonitor(String id, Cache<?, ?> cache) {
        return newObjectMonitor(id, new MonitoredCache(cache));
    }

    /**
     * Register an object with the default registry. Equivalent to
     * {@code DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(obj))}.
     */
    public static void registerObject(Object obj) {
        registerObject(null, obj);
    }

    /**
     * Unregister an object from the default registry. Equivalent to
     * {@code DefaultMonitorRegistry.getInstance().unregister(Monitors.newObjectMonitor(obj))}.
     * @param obj Previously registered using {@code Monitors.registerObject(obj)}
     */
    public static void unregisterObject(Object obj) {
        unregisterObject(null, obj);
    }

    /**
     * Unregister an object from the default registry. Equivalent to
     * {@code DefaultMonitorRegistry.getInstance().unregister(Monitors.newObjectMonitor(id, obj))}.
     * @param obj Previously registered using {@code Monitors.registerObject(id, obj)}
     */
    public static void unregisterObject(String id, Object obj) {
        DefaultMonitorRegistry.getInstance().unregister(newObjectMonitor(id, obj));
    }

    /**
     * Register an object with the default registry. Equivalent to
     * {@code DefaultMonitorRegistry.getInstance().register(Monitors.newObjectMonitor(id, obj))}.
     */
    public static void registerObject(String id, Object obj) {
        DefaultMonitorRegistry.getInstance().register(newObjectMonitor(id, obj));
    }

    /**
     * Returns a new monitor that adds the provided tags to the configuration returned by the
     * wrapped monitor.
     */
    @SuppressWarnings("unchecked")
    static <T> Monitor<T> wrap(TagList tags, Monitor<T> monitor) {
        Monitor<T> m;
        if (monitor instanceof CompositeMonitor<?>) {
            m = new CompositeMonitorWrapper<T>(tags, (CompositeMonitor<T>) monitor);
        } else if (monitor instanceof NumericMonitor<?>) {
            m = (Monitor<T>) new NumericMonitorWrapper(tags, (NumericMonitor<?>) monitor);
        } else {
            m = new MonitorWrapper<T>(tags, monitor);
        }
        return m;
    }

    /** Extract all monitors across class hierarchy. */
    static void addMonitors(List<Monitor<?>> monitors, String id, TagList tags, Object obj) {
        for (Class<?> c = obj.getClass(); c != null; c = c.getSuperclass()) {
            addMonitorFields(monitors, id, tags, obj, c);
            addAnnotatedFields(monitors, id, tags, obj, c);
        }
    }

    /**
     * Extract all fields of {@code obj} that are of type {@link Monitor} and add them to
     * {@code monitors}.
     */
    static void addMonitorFields(
            List<Monitor<?>> monitors, String id, TagList tags, Object obj, Class<?> c) {
        try {
            final SortedTagList.Builder builder = SortedTagList.builder();
            builder.withTag("class", className(obj.getClass()));
            if (tags != null) {
                builder.withTags(tags);
            }
            if (id != null) {
                builder.withTag("id", id);
            }
            final TagList classTags = builder.build();

            final Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (isMonitorType(field.getType())) {
                    field.setAccessible(true);
                    final Monitor<?> m = (Monitor<?>) field.get(obj);
                    if (m == null) {
                        throw new NullPointerException("field " + field.getName()
                            + " in class " + c.getName() + " is null, all monitor fields must be"
                            + " initialized before registering");
                    }
                    monitors.add(wrap(classTags, m));
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Extract all fields/methods of {@code obj} that have a monitor annotation and add them to
     * {@code monitors}.
     */
    static void addAnnotatedFields(
            List<Monitor<?>> monitors, String id, TagList tags, Object obj, Class<?> c) {
        final Class<com.netflix.servo.annotations.Monitor> annoClass =
            com.netflix.servo.annotations.Monitor.class;
        try {
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                final com.netflix.servo.annotations.Monitor anno = field.getAnnotation(annoClass);
                if (anno != null) {
                    final MonitorConfig config =
                        newConfig(obj.getClass(), field.getName(), id, anno, tags);
                    if (anno.type() == DataSourceType.INFORMATIONAL) {
                        monitors.add(new AnnotatedStringMonitor(config, obj, field));
                    } else {
                        checkType(anno, field.getType(), c);
                        monitors.add(new AnnotatedNumberMonitor(config, obj, field));
                    }
                }
            }

            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                final com.netflix.servo.annotations.Monitor anno = method.getAnnotation(annoClass);
                if (anno != null) {
                    final MonitorConfig config =
                        newConfig(obj.getClass(), method.getName(), id, anno, tags);
                    if (anno.type() == DataSourceType.INFORMATIONAL) {
                        monitors.add(new AnnotatedStringMonitor(config, obj, method));
                    } else {
                        checkType(anno, method.getReturnType(), c);
                        monitors.add(new AnnotatedNumberMonitor(config, obj, method));
                    }
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    /** Get tags from annotation. */
    private static TagList getMonitorTags(Object obj) {
        try {
            Class<?> c = obj.getClass();
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                final MonitorTags anno = field.getAnnotation(MonitorTags.class);
                if (anno != null) {
                    field.setAccessible(true);
                    return (TagList) field.get(obj);
                }
            }

            Method[] methods = c.getDeclaredMethods();
            for (Method method : methods) {
                final MonitorTags anno = method.getAnnotation(MonitorTags.class);
                if (anno != null) {
                    method.setAccessible(true);
                    return (TagList) method.invoke(obj);
                }
            }
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }

        return null;
    }

    /** Verify that the type for the annotated field is numeric. */
    private static void checkType(
            com.netflix.servo.annotations.Monitor anno, Class<?> type, Class<?> container) {
        if (!isNumericType(type)) {
            final String msg = "annotation of type " + anno.type().name() + " can only be used"
                + " with numeric values, " + anno.name() + " in class " + container.getName()
                + " is applied to a field or method of type " + type.getName();
            throw new IllegalArgumentException(msg);
        }
    }

    /** Returns true if {@code c} can be assigned to a number. */
    private static boolean isNumericType(Class<?> c) {
        return Number.class.isAssignableFrom(c)
            || double.class == c
            || float.class == c
            || long.class == c
            || int.class == c
            || short.class == c
            || byte.class == c;
    }

    /** Returns true if {@code c} can be assigned to a monitor. */
    private static boolean isMonitorType(Class<?> c) {
        return Monitor.class.isAssignableFrom(c);
    }

    /** Creates a monitor config for a composite object. */
    private static MonitorConfig newObjectConfig(Class<?> c, String id, TagList tags) {
        MonitorConfig.Builder builder = MonitorConfig.builder(id);
        builder.withTag("class", c.getSimpleName());
        if (tags != null) {
            builder.withTags(tags);
        }
        return builder.build();
    }

    private static String className(Class c) {
        final String simpleName = c.getSimpleName();

        return simpleName.isEmpty() ? className(c.getEnclosingClass()) : simpleName;
    }

    /** Creates a monitor config based on an annotation. */
    private static MonitorConfig newConfig(
            Class<?> c,
            String defaultName,
            String id,
            com.netflix.servo.annotations.Monitor anno,
            TagList tags) {
        String name = anno.name();
        if (name.isEmpty()) {
            name = defaultName;
        }
        MonitorConfig.Builder builder = MonitorConfig.builder(name);
        builder.withTag("class", className(c));
        builder.withTag(anno.type());
        builder.withTag(anno.level());
        if (tags != null) {
            builder.withTags(tags);
        }
        if (id != null) {
            builder.withTag("id", id);
        }
        return builder.build();
    }
}
