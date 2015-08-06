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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class that dynamically creates {@link BasicTimer}s based on an arbitrary
 * (name, tagList), or {@link MonitorConfig}. Timers are automatically expired after 15 minutes of
 * inactivity.
 */
public final class DynamicTimer extends AbstractMonitor<Long> implements CompositeMonitor<Long> {
  private static final String DEFAULT_EXPIRATION = "15";
  private static final String DEFAULT_EXPIRATION_UNIT = "MINUTES";
  private static final String CLASS_NAME = DynamicTimer.class.getCanonicalName();
  private static final String EXPIRATION_PROP = CLASS_NAME + ".expiration";
  private static final String EXPIRATION_PROP_UNIT = CLASS_NAME + ".expirationUnit";
  private static final String INTERNAL_ID = "servoTimers";
  private static final MonitorConfig BASE_CONFIG = new MonitorConfig.Builder(INTERNAL_ID).build();

  private static final DynamicTimer INSTANCE = new DynamicTimer();
  private final ExpiringCache<ConfigUnit, Timer> timers;

  static class ConfigUnit {
    private final MonitorConfig config;
    private final TimeUnit unit;

    ConfigUnit(MonitorConfig config, TimeUnit unit) {
      this.config = config;
      this.unit = unit;
    }

    MonitorConfig getConfig() {
      return config;
    }

    TimeUnit getUnit() {
      return unit;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }

      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final ConfigUnit that = (ConfigUnit) o;
      return config.equals(that.config) && unit == that.unit;
    }

    @Override
    public int hashCode() {
      int result = config.hashCode();
      result = 31 * result + unit.hashCode();
      return result;
    }
  }

  private DynamicTimer() {
    super(BASE_CONFIG);
    final String expiration = System.getProperty(EXPIRATION_PROP, DEFAULT_EXPIRATION);
    final String expirationUnit =
        System.getProperty(EXPIRATION_PROP_UNIT, DEFAULT_EXPIRATION_UNIT);
    final long expirationValue = Long.parseLong(expiration);
    final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);
    final long expireAfterMs = expirationUnitValue.toMillis(expirationValue);
    timers = new ExpiringCache<>(expireAfterMs,
        configUnit -> new BasicTimer(configUnit.config, configUnit.unit));
    DefaultMonitorRegistry.getInstance().register(this);
  }

  private Timer get(MonitorConfig config, TimeUnit unit) {
    return timers.get(new ConfigUnit(config, unit));
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to the dynamic timer specified by the given config.
   *
   * @param config Config to identify a particular timer instance to update.
   * @param unit   The unit to use when reporting values to observers. For example if sent to
   *               a typical time series graphing system this would be the unit for the y-axis.
   *               It is generally recommended to use base units for reporting, so
   *               {@link TimeUnit#SECONDS} is the preferred value.
   */
  public static Stopwatch start(MonitorConfig config, TimeUnit unit) {
    return INSTANCE.get(config, unit).start();
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to the dynamic timer specified by the given config. The timer
   * will report the times in milliseconds to observers.
   *
   * @see #start(MonitorConfig, TimeUnit)
   */
  public static Stopwatch start(MonitorConfig config) {
    return INSTANCE.get(config, TimeUnit.MILLISECONDS).start();
  }

  /**
   * Record result to the dynamic timer indicated by the provided config
   * with a TimeUnit of milliseconds.
   */
  public static void record(MonitorConfig config, long duration) {
    INSTANCE.get(config, TimeUnit.MILLISECONDS).record(duration, TimeUnit.MILLISECONDS);
  }

  /**
   * Record a duration to the dynamic timer indicated by the provided config.
   * The units in which the timer is reported and the duration unit  are the same.
   *
   * @deprecated Use {@link DynamicTimer#record(MonitorConfig, java.util.concurrent.TimeUnit,
   * long, java.util.concurrent.TimeUnit)} instead.
   * The new method allows you to be specific about the units used for reporting the timer and
   * the units in which the duration is measured.
   */
  public static void record(MonitorConfig config, long duration, TimeUnit unit) {
    INSTANCE.get(config, unit).record(duration, unit);
  }

  /**
   * Record a duration to the dynamic timer indicated by the provided config/reportUnit.
   *
   * @param config       Config to identify a particular timer instance to update.
   * @param reportUnit   The unit to use when reporting values to observers. For example if sent to
   *                     a typical time series graphing system this would be the unit
   *                     for the y-axis.
   *                     It is generally recommended to use base units for reporting, so
   *                     {@link TimeUnit#SECONDS} is the preferred value.
   * @param duration     Measured duration to record.
   * @param durationUnit Unit for the measured duration. This should typically be the unit used for
   *                     timing source. For example if using {@link System#nanoTime()}
   *                     the unit would be nanoseconds.
   */
  public static void record(MonitorConfig config, TimeUnit reportUnit, long duration,
                            TimeUnit durationUnit) {
    INSTANCE.get(config, reportUnit).record(duration, durationUnit);
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to the dynamic timer specified by the given name, and sequence of (key,
   * value) pairs. The timer uses a TimeUnit of milliseconds.
   */
  public static Stopwatch start(String name, String... tags) {
    final MonitorConfig.Builder configBuilder = MonitorConfig.builder(name);
    Preconditions.checkArgument(tags.length % 2 == 0,
        "The sequence of (key, value) pairs must have even size: one key, one value");
    for (int i = 0; i < tags.length; i += 2) {
      configBuilder.withTag(tags[i], tags[i + 1]);
    }

    return INSTANCE.get(configBuilder.build(), TimeUnit.MILLISECONDS).start();
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to the dynamic timer specified by the given config. The timer
   * uses a TimeUnit of milliseconds.
   */
  public static Stopwatch start(String name, TagList list) {
    final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
    return INSTANCE.get(config, TimeUnit.MILLISECONDS).start();
  }

  /**
   * Returns a stopwatch that has been started and will automatically
   * record its result to the dynamic timer specified by the given config. The timer
   * uses a TimeUnit of milliseconds.
   */
  public static Stopwatch start(String name, TagList list, TimeUnit unit) {
    final MonitorConfig config = new MonitorConfig.Builder(name).withTags(list).build();
    return INSTANCE.get(config, unit).start();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public List<Monitor<?>> getMonitors() {
    List list = timers.values();
    return (List<Monitor<?>>) list;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIdx) {
    return (long) timers.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "DynamicCounter{baseConfig" + BASE_CONFIG
        + ", totalTimers=" + timers.size()
        + ", timers=" + timers + '}';
  }
}
