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
package com.netflix.servo.monitor;

import com.google.common.base.Function;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.tag.TaggingContext;
import com.netflix.servo.util.UnmodifiableList;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class used to simplify creation of contextual monitors.
 */
public abstract class AbstractContextualMonitor<T, M extends Monitor<T>>
    implements CompositeMonitor<T>, SpectatorMonitor {

  /**
   * Base configuration shared across all contexts.
   */
  protected final MonitorConfig baseConfig;

  /**
   * Additional tags to add to spectator monitors.
   */
  protected TagList spectatorTags = BasicTagList.EMPTY;

  /**
   * Context to query when accessing a monitor.
   */
  protected final TaggingContext context;

  /**
   * Factory function used to create a new instance of a monitor.
   */
  protected final Function<MonitorConfig, M> newMonitor;

  /**
   * Thread-safe map keeping track of the distinct monitors that have been created so far.
   */
  protected final ConcurrentMap<MonitorConfig, M> monitors;

  /**
   * Create a new instance of the monitor.
   *
   * @param baseConfig shared configuration
   * @param context    provider for context specific tags
   * @param newMonitor function to create new monitors
   */
  protected AbstractContextualMonitor(
      MonitorConfig baseConfig,
      TaggingContext context,
      Function<MonitorConfig, M> newMonitor) {
    this.baseConfig = baseConfig;
    this.context = context;
    this.newMonitor = newMonitor;

    monitors = new ConcurrentHashMap<>();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeSpectator(TagList tags) {
    spectatorTags = tags;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue() {
    return getValue(0);
  }

  /**
   * Returns a monitor instance for the current context. If no monitor exists for the current
   * context then a new one will be created.
   */
  protected M getMonitorForCurrentContext() {
    MonitorConfig contextConfig = getConfig();
    M monitor = monitors.get(contextConfig);
    if (monitor == null) {
      M newMon = newMonitor.apply(contextConfig);
      if (newMon instanceof SpectatorMonitor) {
        ((SpectatorMonitor) newMon).initializeSpectator(spectatorTags);
      }
      monitor = monitors.putIfAbsent(contextConfig, newMon);
      if (monitor == null) {
        monitor = newMon;
      }
    }
    return monitor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MonitorConfig getConfig() {
    TagList contextTags = context.getTags();
    return MonitorConfig.builder(baseConfig.getName())
        .withTags(baseConfig.getTags())
        .withTags(contextTags)
        .build();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    return UnmodifiableList.<Monitor<?>>copyOf(monitors.values());
  }
}
