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

import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.spectator.api.Counter;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.Gauge;
import com.netflix.spectator.api.Id;
import com.netflix.spectator.api.NoopRegistry;
import com.netflix.spectator.api.Registry;
import com.netflix.spectator.api.Timer;
import com.netflix.spectator.api.patterns.PolledMeter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

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

  private static volatile Registry registry = new NoopRegistry();

  /**
   * Set the registry to use. By default it will use the NoopRegistry.
   */
  public static void setRegistry(Registry registry) {
    SpectatorContext.registry = registry;
  }

  /**
   * Get the registry that was configured.
   */
  public static Registry getRegistry() {
    return registry;
  }

  /** Create a gauge based on the config. */
  public static Gauge gauge(MonitorConfig config) {
    return registry.gauge(createId(config));
  }

  /** Create a max gauge based on the config. */
  public static Gauge maxGauge(MonitorConfig config) {
    return registry.maxGauge(createId(config));
  }

  /** Create a counter based on the config. */
  public static Counter counter(MonitorConfig config) {
    return registry.counter(createId(config));
  }

  /** Create a timer based on the config. */
  public static Timer timer(MonitorConfig config) {
    return registry.timer(createId(config));
  }

  /** Create a distribution summary based on the config. */
  public static DistributionSummary distributionSummary(MonitorConfig config) {
    return registry.distributionSummary(createId(config));
  }

  /** Convert servo config to spectator id. */
  public static Id createId(MonitorConfig config) {
    return registry
        .createId(config.getName())
        .withTags(config.getTags().asMap());
  }

  /** Dedicated thread pool for polling user defined functions registered as gauges. */
  public static ScheduledExecutorService gaugePool() {
    return GAUGE_POOL;
  }

  /** Create builder for a polled gauge based on the config. */
  public static PolledMeter.Builder polledGauge(MonitorConfig config) {
    return PolledMeter.using(registry)
        .withId(createId(config))
        .scheduleOn(GAUGE_POOL);
  }
}
