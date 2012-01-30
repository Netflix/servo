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

import com.netflix.servo.BasicTagList;
import com.netflix.servo.TagList;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.publish.MetricConfig;

/**
 * Helper class for basic counters that can be actively updated and tagged
 * based on context. When one of the increment methods is called a counter will
 * be created automatically if one does not already exist and registered with
 * the {@link com.netflix.servo.DefaultMonitorRegistry}.
 */
public final class Counters {

    private static final LoadingCache<MetricConfig,BasicCounter> COUNTERS =
        CacheBuilder.newBuilder().build(
        new CacheLoader<MetricConfig,BasicCounter>() {
            @Override
            public BasicCounter load(MetricConfig key) {
                BasicCounter counter = new BasicCounter(key);
                DefaultMonitorRegistry.getInstance().registerObject(counter);
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
        increment(new MetricConfig(name), 1);
    }

    /**
     * Increment the counter with the given name and only tags specified in the
     * context.
     *
     * @param name   name of the counter to increment
     * @param delta  the amount to increment the counter by
     */
    public static void increment(String name, int delta) {
        increment(new MetricConfig(name), delta);
    }

    /**
     * Increment the counter with the given name and tags.
     *
     * @param name   name of the counter to increment
     * @param tags   tags to associate with the counter
     */
    public static void increment(String name, TagList tags) {
        increment(new MetricConfig(name, tags), 1);
    }

    /**
     * Increment the counter with the given name and tags.
     *
     * @param name   name of the counter to increment
     * @param tags   tags to associate with the counter
     * @param delta  the amount to increment the counter by
     */
    public static void increment(String name, TagList tags, int delta) {
        increment(new MetricConfig(name, tags), delta);
    }

    /**
     * Increment the counter with the given config.
     *
     * @param config  config of the counter to increment
     * @param delta   the amount to increment the counter by
     */
    public static void increment(MetricConfig config, int delta) {
        TagList cxtTags = TaggingContext.getTags();
        if (cxtTags != null) {
            String name = config.getName();
            TagList newTags = BasicTagList.concat(config.getTags(), cxtTags);
            MetricConfig newConfig = new MetricConfig(name, newTags);
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
