/*
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.ExpiringCache;
import com.netflix.servo.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that dynamically creates counters based on an arbitrary (name, tagList), or
 * {@link MonitorConfig}. Counters are automatically expired after 15 minutes of inactivity.
 */
public final class DynamicCounter extends AbstractMonitor<Long> implements CompositeMonitor<Long> {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicCounter.class);
  private static final String DEFAULT_EXPIRATION = "15";
  private static final String DEFAULT_EXPIRATION_UNIT = "MINUTES";
  private static final String CLASS_NAME = DynamicCounter.class.getCanonicalName();
  private static final String EXPIRATION_PROP = CLASS_NAME + ".expiration";
  private static final String EXPIRATION_PROP_UNIT = CLASS_NAME + ".expirationUnit";
  private static final String INTERNAL_ID = "servoCounters";
  private static final MonitorConfig BASE_CONFIG = new MonitorConfig.Builder(INTERNAL_ID).build();

  private static final DynamicCounter INSTANCE = new DynamicCounter();

  private final ExpiringCache<MonitorConfig, Counter> counters;

  private DynamicCounter() {
    super(BASE_CONFIG);
    final String expiration = System.getProperty(EXPIRATION_PROP, DEFAULT_EXPIRATION);
    final String expirationUnit =
        System.getProperty(EXPIRATION_PROP_UNIT, DEFAULT_EXPIRATION_UNIT);
    final long expirationValue = Long.parseLong(expiration);
    final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);
    final long expireAfterMs = expirationUnitValue.toMillis(expirationValue);
    counters = new ExpiringCache<>(expireAfterMs, StepCounter::new);
    DefaultMonitorRegistry.getInstance().register(this);
  }

  private Counter get(final MonitorConfig config) {
    return counters.get(config);
  }

  /**
   * Increment a counter based on a given {@link MonitorConfig}.
   */
  public static void increment(MonitorConfig config) {
    INSTANCE.get(config).increment();
  }

  /**
   * Increment a counter specified by a name, and a sequence of (key, value) pairs.
   */
  public static void increment(String name, String... tags) {
    final MonitorConfig.Builder configBuilder = MonitorConfig.builder(name);
    Preconditions.checkArgument(tags.length % 2 == 0,
        "The sequence of (key, value) pairs must have even size: one key, one value");
    try {
      for (int i = 0; i < tags.length; i += 2) {
        configBuilder.withTag(tags[i], tags[i + 1]);
      }
      increment(configBuilder.build());
    } catch (IllegalArgumentException e) {
      LOGGER.warn("Failed to get a counter to increment: {}", e.getMessage());
    }
  }


  /**
   * Increment a counter based on a given {@link MonitorConfig} by a given delta.
   *
   * @param config The monitoring config
   * @param delta  The amount added to the current value
   */
  public static void increment(MonitorConfig config, long delta) {
    INSTANCE.get(config).increment(delta);
  }

  /**
   * Increment the counter for a given name, tagList.
   */
  public static void increment(String name, TagList list) {
    final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
    increment(config);
  }

  /**
   * Increment the counter for a given name, tagList by a given delta.
   */
  public static void increment(String name, TagList list, long delta) {
    final MonitorConfig config = MonitorConfig.builder(name).withTags(list).build();
    increment(config, delta);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<Monitor<?>> getMonitors() {
    List list = counters.values();
    return (List<Monitor<?>>) list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIndex) {
    return (long) counters.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "DynamicCounter{baseConfig" + BASE_CONFIG
        + ", totalCounters=" + counters.size()
        + ", counters=" + counters + '}';
  }
}
