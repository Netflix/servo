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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * This class creates a {@link java.lang.reflect.Proxy} monitor that tracks all calls to methods
 * of an interface.
 *
 * <pre>
 *   IDummy dummy = TimedInterface.newProxy(IDummy.class, new DummyImpl(), "id");
 *   DefaultMonitorRegistry.getInstance().register((CompositeMonitor)dummy);
 * </pre>
 *
 * <p>
 * All calls to methods implemented by IDummy would have an associated BasicTimer with them. The
 * name for the {@link CompositeMonitor} is the name of the method. Additional tags are added:
 * <ul>
 * <li><code>interface</code> interface being implemented.
 * <li><code>class</code> simple name of the concrete class implementing the interface.
 * <li><code>id</code> (Optional) An identifier for this particular instance.
 * </ul>
 * </p>
 */
public final class TimedInterface {

    static final String TIMED_INTERFACE = "TimedInterface";
    static final String INTERFACE_TAG = "interface";
    static final String CLASS_TAG = "class";
    static final String ID_TAG = "id";

    private static class TimedHandler<T> implements InvocationHandler, CompositeMonitor<Long> {
        final T concrete;

        /**
         * {@inheritDoc}
         */
        @Override
        public List<Monitor<?>> getMonitors() {
            final ConcurrentMap<String, Timer> timersMap = timers.asMap();
            return ImmutableList.<Monitor<?>>copyOf(timersMap.values());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Long getValue() {
            return (long) timers.asMap().size();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public MonitorConfig getConfig() {
            return baseConfig;
        }

        final LoadingCache<String, Timer> timers;
        final MonitorConfig baseConfig;
        final TagList baseTagList;

        TimedHandler(Class<T> ctype, T concrete, String id) {
            this.concrete = concrete;
            BasicTagList tagList = BasicTagList.of(
                    INTERFACE_TAG, ctype.getSimpleName(),
                    CLASS_TAG, concrete.getClass().getSimpleName());
            if (id != null) {
                tagList = tagList.copy(ID_TAG, id);
            }
            baseTagList = tagList;
            baseConfig = MonitorConfig.builder(TIMED_INTERFACE).withTags(baseTagList).build();

            timers = CacheBuilder.newBuilder().build(new CacheLoader<String, Timer>() {
                @Override
                public Timer load(String method) throws Exception {
                    final MonitorConfig config = MonitorConfig.builder(method)
                        .withTags(baseTagList)
                        .build();
                    return new BasicTimer(config);
                }
            });
        }


        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // if the method is one of the CompositeMonitor interface
            final Class<?> declaringClass = method.getDeclaringClass();
            if (declaringClass.isAssignableFrom(CompositeMonitor.class)) {
                return method.invoke(this);
            }
            final String methodName = method.getName();
            final Timer timer = timers.get(methodName);
            final Stopwatch stopwatch = timer.start();
            try {
                return method.invoke(concrete, args);
            } finally {
                stopwatch.stop();
            }
        }
    }

    private TimedInterface() {
    }

    /**
     * Creates a new TimedInterface for a given interface <code>ctype</code> with a concrete class
     * <code>concrete</code> and a specific id. The id can be used to distinguish among multiple
     * objects with the same concrete class.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newProxy(Class<T> ctype, T concrete, String id) {
        final InvocationHandler handler = new TimedHandler(ctype, concrete, id);
        final Class[] types = new Class[] {ctype, CompositeMonitor.class};
        return (T) Proxy.newProxyInstance(ctype.getClassLoader(), types, handler);
    }


    /**
     * Creates a new TimedInterface for a given interface <code>ctype</code> with a concrete class
     * <code>concrete</code>.
     */
    public static <T> T newProxy(Class<T> ctype, T concrete) {
        return newProxy(ctype, concrete, null);
    }
}
