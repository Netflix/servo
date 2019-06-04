/*
 * Copyright 2011-2018 Netflix, Inc.
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
package com.netflix.servo;

import com.netflix.servo.monitor.BasicCompositeMonitor;
import com.netflix.servo.monitor.CompositeMonitor;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Pollers;
import com.netflix.servo.monitor.SpectatorMonitor;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.spectator.api.AbstractTimer;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.Measurement;
import com.netflix.spectator.api.Meter;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * Helper that can be used to delegate to spectator. Other than calling
 * {@link #setRegistry(Registry)} to set the registry to use, it is only intended for use
 * within servo.
 */
public final class SpectatorContext {

  private SpectatorContext() {
  }

  private static final ScheduledExecutorService GAUGE_POOL = Executors.newScheduledThreadPool(
      2,
      task -> {
        Thread t = new Thread(task, "servo-gauge-poller");
        t.setDaemon(true);
        return t;
      }
  );

  private static final Logger LOGGER = LoggerFactory.getLogger(SpectatorContext.class);

  private static volatile Registry registry = new NoopRegistry();

  // Used to keep track of where the registry was being set. This can be useful to help
  // debug issues if missing metrics due to it being set from multiple places.
  private static volatile Exception initStacktrace = null;

  /**
   * Set the registry to use. By default it will use the NoopRegistry.
   */
  public static void setRegistry(Registry registry) {
    SpectatorContext.registry = registry;
    // Ignore if overwriting with the global registry. In some cases it is necessary to set
    // the context for Servo early before a proper registry can be created via injection. In
    // that case the global registry is the best option. If it is later overwritten with the
    // registry created by the injector, then that should not trigger a warning to the user.
    if (registry instanceof NoopRegistry || isGlobal(registry)) {
      initStacktrace = null;
    } else {
      Exception cause = initStacktrace;
      Exception e = new IllegalStateException(
          "called SpectatorContext.setRegistry(" + registry.getClass().getName() + ")",
          cause);
      e.fillInStackTrace();
      initStacktrace = e;
      if (cause != null) {
        LOGGER.warn("Registry used with Servo's SpectatorContext has been overwritten. This could "
            + "result in missing metrics.", e);
      }
    }
  }

  private static boolean isGlobal(Registry registry) {
    // Use identity check to see it is the global instance
    return registry == Spectator.globalRegistry();
  }

  /**
   * Get the registry that was configured.
   */
  public static Registry getRegistry() {
    return registry;
  }

  /** Create a gauge based on the config. */
  public static LazyGauge gauge(MonitorConfig config) {
    return new LazyGauge(Registry::gauge, registry, createId(config));
  }

  /** Create a max gauge based on the config. */
  public static LazyGauge maxGauge(MonitorConfig config) {
    return new LazyGauge(Registry::maxGauge, registry, createId(config));
  }

  /** Create a counter based on the config. */
  public static LazyCounter counter(MonitorConfig config) {
    return new LazyCounter(registry, createId(config));
  }

  /** Create a timer based on the config. */
  public static LazyTimer timer(MonitorConfig config) {
    return new LazyTimer(registry, createId(config));
  }

  /** Create a distribution summary based on the config. */
  public static LazyDistributionSummary distributionSummary(MonitorConfig config) {
    return new LazyDistributionSummary(registry, createId(config));
  }

  /** Convert servo config to spectator id. */
  public static Id createId(MonitorConfig config) {
    // Need to ensure that Servo type tag is removed to avoid incorrectly reprocessing the
    // data in later transforms
    Map<String, String> tags = new HashMap<>(config.getTags().asMap());
    tags.remove("type");
    return registry
        .createId(config.getName())
        .withTags(tags);
  }

  /** Dedicated thread pool for polling user defined functions registered as gauges. */
  public static ScheduledExecutorService gaugePool() {
    return GAUGE_POOL;
  }

  /** Create builder for a polled gauge based on the config. */
  public static PolledMeter.Builder polledGauge(MonitorConfig config) {
    long delayMillis = Math.max(Pollers.getPollingIntervals().get(0) - 1000, 5000);
    Id id = createId(config);
    PolledMeter.remove(registry, id);
    return PolledMeter.using(registry)
        .withId(id)
        .withDelay(Duration.ofMillis(delayMillis))
        .scheduleOn(gaugePool());
  }

  /** Register a custom monitor. */
  public static void register(Monitor<?> monitor) {
    if (monitor instanceof SpectatorMonitor) {
      ((SpectatorMonitor) monitor).initializeSpectator(BasicTagList.EMPTY);
    } else if (!isEmptyComposite(monitor)) {
      ServoMeter m = new ServoMeter(monitor);
      PolledMeter.remove(registry, m.id());
      PolledMeter.monitorMeter(registry, m);
      monitorMonitonicValues(monitor);
    }
  }

  /**
   * A basic composite has an immutable list, if it is empty then will never provide any
   * useful monitors. This can happen if a monitor type is used with Monitors.registerObject.
   */
  private static boolean isEmptyComposite(Monitor<?> monitor) {
    return (monitor instanceof BasicCompositeMonitor)
        && ((BasicCompositeMonitor) monitor).getMonitors().isEmpty();
  }

  private static boolean isCounter(MonitorConfig config) {
    return "COUNTER".equals(config.getTags().getValue("type"));
  }

  private static void monitorMonitonicValues(Monitor<?> monitor) {
    if (!(monitor instanceof SpectatorMonitor)) {
      if (monitor instanceof CompositeMonitor<?>) {
        CompositeMonitor<?> cm = (CompositeMonitor<?>) monitor;
        for (Monitor<?> m : cm.getMonitors()) {
          monitorMonitonicValues(m);
        }
      } else if (isCounter(monitor.getConfig())) {
        polledGauge(monitor.getConfig())
            .monitorMonotonicCounter(monitor, m -> ((Number) m.getValue()).longValue());
      }
    }
  }

  /** Unregister a custom monitor. */
  public static void unregister(Monitor<?> monitor) {
    PolledMeter.remove(registry, createId(monitor.getConfig()));
  }

  private static class ServoMeter implements Meter {

    private final Id id;
    private final Monitor<?> monitor;

    ServoMeter(Monitor<?> monitor) {
      this.id = createId(monitor.getConfig());
      this.monitor = monitor;
    }

    @Override public Id id() {
      return id;
    }

    private void addMeasurements(Monitor<?> m, List<Measurement> measurements) {
      // Skip any that will report directly
      if (!(m instanceof SpectatorMonitor)) {
        if (m instanceof CompositeMonitor<?>) {
          CompositeMonitor<?> cm = (CompositeMonitor<?>) m;
          for (Monitor<?> v : cm.getMonitors()) {
            addMeasurements(v, measurements);
          }
        } else if (!isCounter(m.getConfig())) {
          try {
            Object obj = m.getValue();
            if (obj instanceof Number) {
              double value = ((Number) obj).doubleValue();
              // timestamp will get ignored as the value will get forwarded to a gauge
              Measurement v = new Measurement(createId(m.getConfig()), 0L, value);
              measurements.add(v);
            }
          } catch (Throwable t) {
            LOGGER.warn("Exception while querying user defined gauge ({}), "
                + "the value will be ignored. The owner of the user defined "
                + "function should fix it to not propagate an exception.", m.getConfig(), t);
          }
        }
      }
    }

    @Override public Iterable<Measurement> measure() {
      List<Measurement> measurements = new ArrayList<>();
      addMeasurements(monitor, measurements);
      return measurements;
    }

    @Override public boolean hasExpired() {
      return false;
    }
  }

  /** Create a counter when it is first updated. */
  public static class LazyCounter implements Counter {

    private final Registry registry;
    private volatile Id id;
    private volatile Counter counter;

    LazyCounter(Registry registry, Id id) {
      this.registry = registry;
      this.id = id;
    }

    /** Set a new id to use. */
    public void setId(Id id) {
      this.id = id;
      this.counter = null;
    }

    private Counter get() {
      Counter c = counter;
      if (c == null) {
        c = registry.counter(id);
        counter = c;
      }
      return c;
    }

    @Override public void add(double amount) {
      get().add(amount);
    }

    @Override public double actualCount() {
      return get().actualCount();
    }

    @Override public Id id() {
      return get().id();
    }

    @Override public Iterable<Measurement> measure() {
      return get().measure();
    }

    @Override public boolean hasExpired() {
      return get().hasExpired();
    }
  }

  /** Create a timer when it is first updated. */
  public static class LazyTimer extends AbstractTimer implements Timer {

    private final Registry registry;
    private volatile Id id;
    private volatile Timer timer;

    LazyTimer(Registry registry, Id id) {
      super(registry.clock());
      this.registry = registry;
      this.id = id;
    }

    /** Set a new id to use. */
    public void setId(Id id) {
      this.id = id;
      this.timer = null;
    }

    private Timer get() {
      Timer t = timer;
      if (t == null) {
        t = registry.timer(id);
        timer = t;
      }
      return t;
    }

    @Override public Id id() {
      return get().id();
    }

    @Override public Iterable<Measurement> measure() {
      return get().measure();
    }

    @Override public boolean hasExpired() {
      return get().hasExpired();
    }

    @Override public void record(long amount, TimeUnit unit) {
      get().record(amount, unit);
    }

    @Override public long count() {
      return get().count();
    }

    @Override public long totalTime() {
      return get().totalTime();
    }
  }

  /** Create a distribution summary when it is first updated. */
  public static class LazyDistributionSummary implements DistributionSummary {

    private final Registry registry;
    private volatile Id id;
    private volatile DistributionSummary summary;

    LazyDistributionSummary(Registry registry, Id id) {
      this.registry = registry;
      this.id = id;
    }

    /** Set a new id to use. */
    public void setId(Id id) {
      this.id = id;
      this.summary = null;
    }

    private DistributionSummary get() {
      DistributionSummary s = summary;
      if (s == null) {
        s = registry.distributionSummary(id);
        summary = s;
      }
      return s;
    }

    @Override public Id id() {
      return get().id();
    }

    @Override public Iterable<Measurement> measure() {
      return get().measure();
    }

    @Override public boolean hasExpired() {
      return get().hasExpired();
    }

    @Override public void record(long amount) {
      get().record(amount);
    }

    @Override public long count() {
      return get().count();
    }

    @Override public long totalAmount() {
      return get().totalAmount();
    }
  }

  /** Create a gauge when it is first updated. */
  public static class LazyGauge implements Gauge {

    private final Registry registry;
    private final BiFunction<Registry, Id, Gauge> factory;
    private volatile Id id;
    private volatile Gauge gauge;

    LazyGauge(BiFunction<Registry, Id, Gauge> factory, Registry registry, Id id) {
      this.registry = registry;
      this.factory = factory;
      this.id = id;
    }

    /** Set a new id to use. */
    public void setId(Id id) {
      this.id = id;
      this.gauge = null;
    }

    private Gauge get() {
      Gauge g = gauge;
      if (g == null) {
        g = factory.apply(registry, id);
        gauge = g;
      }
      return g;
    }

    @Override public Id id() {
      return get().id();
    }

    @Override public Iterable<Measurement> measure() {
      return get().measure();
    }

    @Override public boolean hasExpired() {
      return get().hasExpired();
    }

    @Override public void set(double v) {
      get().set(v);
    }

    @Override public double value() {
      return get().value();
    }
  }
}
