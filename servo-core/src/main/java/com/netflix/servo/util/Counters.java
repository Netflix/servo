/*
 * #%L
 * servo-core
 * %%
 * Copyright (C) 2011 - 2012 Netflix
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
package com.netflix.servo.util;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.MonitorContext;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.TagList;

/**
 * Helper class for basic counters that can be actively updated and tagged
 * based on context. When one of the increment methods is called a counter will
 * be created automatically if one does not already exist and registered with
 * the {@link com.netflix.servo.DefaultMonitorRegistry}.
 */
public final class Counters {

    private static final LoadingCache<MonitorContext,BasicCounter> COUNTERS =
        CacheBuilder.newBuilder().build(
        new CacheLoader<MonitorContext,BasicCounter>() {
            @Override
            public BasicCounter load(MonitorContext key) {
                BasicCounter counter = new BasicCounter(key);
                DefaultMonitorRegistry.getInstance().registerAnnotatedObject(counter);
                return counter;
            }
        });

    private Counters() {
    }

    /**
     * Increment the counter with the given name and only tags specified in the
     * context.
     *
     * @param name  name of the counter to increment
     */
    public static void increment(String name) {
        increment(new MonitorContext.Builder(name).build(), 1);
    }

    /**
     * Increment the counter with the given name and only tags specified in the
     * context.
     *
     * @param name   name of the counter to increment
     * @param delta  the amount to increment the counter by
     */
    public static void increment(String name, long delta) {
        increment(new MonitorContext.Builder(name).build(), delta);
    }

    /**
     * Increment the counter with the given name and tags.
     *
     * @param name   name of the counter to increment
     * @param tags   tags to associate with the counter
     */
    public static void increment(String name, TagList tags) {
        increment(new MonitorContext.Builder(name).withTags(tags).build(), 1);
    }

    /**
     * Increment the counter with the given name and tags.
     *
     * @param name   name of the counter to increment
     * @param tags   tags to associate with the counter
     * @param delta  the amount to increment the counter by
     */
    public static void increment(String name, TagList tags, long delta) {
        increment(new MonitorContext.Builder(name).withTags(tags).build(), delta);
    }

    /**
     * Increment the counter with the given config.
     *
     * @param config  config of the counter to increment
     * @param delta   the amount to increment the counter by
     */
    public static void increment(MonitorContext config, long delta) {
        TagList cxtTags = TaggingContext.getTags();
        if (cxtTags != null) {
            String name = config.getName();
            TagList newTags = SortedTagList.builder().withTags(config.getTags()).withTags(cxtTags).build();
            MonitorContext newConfig = new MonitorContext.Builder(name).withTags(newTags).build();
            COUNTERS.getUnchecked(newConfig).increment(delta);
        } else {
            COUNTERS.getUnchecked(config).increment(delta);
        }
    }

    /** Clear out all counters. */
    public static void reset() {
        COUNTERS.invalidateAll();
    }
}
