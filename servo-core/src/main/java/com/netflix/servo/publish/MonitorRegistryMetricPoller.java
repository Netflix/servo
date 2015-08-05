/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.publish;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.MonitorRegistry;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;
import com.netflix.servo.util.ThreadFactories;
import com.netflix.servo.util.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Poller for fetching {@link com.netflix.servo.annotations.Monitor} metrics
 * from a monitor registry.
 */
public final class MonitorRegistryMetricPoller implements MetricPoller {

  private static final Logger LOGGER = LoggerFactory.getLogger(MonitorRegistryMetricPoller.class);

  private final MonitorRegistry registry;

  private final long cacheTTL;

  private final AtomicReference<List<Monitor<?>>> cachedMonitors =
      new AtomicReference<>();

  private final AtomicLong cacheLastUpdateTime = new AtomicLong(0L);

  // Put limit on fetching the monitor value in-case someone does something silly like call a
  // remote service inline
  private final TimeLimiter limiter;
  private final ExecutorService service;

  private final Clock clock;

  /**
   * Creates a new instance using {@link com.netflix.servo.DefaultMonitorRegistry}.
   */
  public MonitorRegistryMetricPoller() {
    this(DefaultMonitorRegistry.getInstance(), 0L, TimeUnit.MILLISECONDS, true);
  }

  /**
   * Creates a new instance using the specified registry.
   *
   * @param registry registry to query for annotated objects
   */
  public MonitorRegistryMetricPoller(MonitorRegistry registry) {
    this(registry, 0L, TimeUnit.MILLISECONDS, true, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance using the specified registry and a time limiter.
   *
   * @param registry registry to query for annotated objects
   * @param cacheTTL how long to cache the filtered monitor list from the registry
   * @param unit     time unit for the cache ttl
   */
  public MonitorRegistryMetricPoller(MonitorRegistry registry, long cacheTTL, TimeUnit unit) {
    this(registry, cacheTTL, unit, true);
  }

  /**
   * Creates a new instance using the specified registry.
   *
   * @param registry   registry to query for annotated objects
   * @param cacheTTL   how long to cache the filtered monitor list from the registry
   * @param unit       time unit for the cache ttl
   * @param useLimiter whether to use a time limiter for getting the values from the monitors
   */
  public MonitorRegistryMetricPoller(MonitorRegistry registry, long cacheTTL, TimeUnit unit,
                                     boolean useLimiter) {
    this(registry, cacheTTL, unit, useLimiter, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance using the specified registry.
   *
   * @param registry   registry to query for annotated objects
   * @param cacheTTL   how long to cache the filtered monitor list from the registry
   * @param unit       time unit for the cache ttl
   * @param useLimiter whether to use a time limiter for getting the values from the monitors
   * @param clock      clock instance to use to get the time
   */
  public MonitorRegistryMetricPoller(
      MonitorRegistry registry, long cacheTTL, TimeUnit unit, boolean useLimiter,
      Clock clock) {
    this.registry = registry;
    this.cacheTTL = TimeUnit.MILLISECONDS.convert(cacheTTL, unit);
    this.clock = clock;

    if (useLimiter) {
      final ThreadFactory factory =
          ThreadFactories.withName("ServoMonitorGetValueLimiter-%d");
      service = Executors.newSingleThreadExecutor(factory);
      limiter = new TimeLimiter(service);
    } else {
      service = null;
      limiter = null;
    }
  }

  private Object getValue(Monitor<?> monitor) {
    try {
      if (limiter != null) {
        final MonitorValueCallable c = new MonitorValueCallable(monitor);
        return limiter.callWithTimeout(c, 1, TimeUnit.SECONDS);
      } else {
        return monitor.getValue();
      }
    } catch (TimeLimiter.UncheckedTimeoutException e) {
      LOGGER.warn("timeout trying to get value for {}", monitor.getConfig());
    } catch (Exception e) {
      LOGGER.warn("failed to get value for " + monitor.getConfig(), e);
    }
    return null;
  }

  private void getMonitors(List<Monitor<?>> monitors, MetricFilter filter, Monitor<?> monitor) {
    if (monitor instanceof CompositeMonitor<?>) {
      for (Monitor<?> m : ((CompositeMonitor<?>) monitor).getMonitors()) {
        getMonitors(monitors, filter, m);
      }
    } else if (filter.matches(monitor.getConfig())) {
      monitors.add(monitor);
    }
  }

  private void refreshMonitorCache(MetricFilter filter) {
    final long age = System.currentTimeMillis() - cacheLastUpdateTime.get();
    if (age > cacheTTL) {
      List<Monitor<?>> monitors = new ArrayList<>();
      for (Monitor<?> monitor : registry.getRegisteredMonitors()) {
        try {
          getMonitors(monitors, filter, monitor);
        } catch (Exception e) {
          LOGGER.warn("failed to get monitors for composite " + monitor.getConfig(), e);
        }
      }
      cacheLastUpdateTime.set(clock.now());
      cachedMonitors.set(monitors);
      LOGGER.debug("cache refreshed, {} monitors matched filter, previous age {} seconds",
          monitors.size(), age / 1000);
    } else {
      LOGGER.debug("cache age of {} seconds is within ttl of {} seconds",
          age / 1000, cacheTTL / 1000);
    }
  }

  /**
   * {@inheritDoc}
   */
  public List<Metric> poll(MetricFilter filter) {
    return poll(filter, false);
  }

  /**
   * {@inheritDoc}
   */
  public List<Metric> poll(MetricFilter filter, boolean reset) {
    refreshMonitorCache(filter);
    List<Monitor<?>> monitors = cachedMonitors.get();
    List<Metric> metrics = new ArrayList<>(monitors.size());
    for (Monitor<?> monitor : monitors) {
      Object v = getValue(monitor);
      if (v != null) {
        metrics.add(new Metric(monitor.getConfig(), clock.now(), v));
      }
    }
    return metrics;
  }

  /**
   * Shutsdown the thread executor used for time limiting the get value calls. It is a good idea
   * to call this and explicitly cleanup the thread. In most cases the threads will be cleaned
   * up when the executor is garbage collected if shutdown is not called explicitly.
   */
  public void shutdown() {
    if (service != null) {
      service.shutdownNow();
    }
  }

  private static class MonitorValueCallable implements Callable<Object> {

    private final Monitor<?> monitor;

    public MonitorValueCallable(Monitor<?> monitor) {
      this.monitor = monitor;
    }

    public Object call() throws Exception {
      return monitor.getValue();
    }
  }
}
