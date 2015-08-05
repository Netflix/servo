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
package com.netflix.servo.monitor;

import com.netflix.servo.tag.TagList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Wraps another composite monitor object providing an alternative configuration.
 */
class CompositeMonitorWrapper<T> extends AbstractMonitor<T> implements CompositeMonitor<T> {

  private final TagList tags;
  private final CompositeMonitor<T> monitor;

  /**
   * Creates a new instance of the wrapper.
   */
  public CompositeMonitorWrapper(TagList tags, CompositeMonitor<T> monitor) {
    super(monitor.getConfig().withAdditionalTags(tags));
    this.tags = tags;
    this.monitor = monitor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    List<Monitor<?>> monitors = monitor.getMonitors();
    List<Monitor<?>> wrappedMonitors = new ArrayList<>(monitors.size());
    for (Monitor<?> m : monitors) {
      wrappedMonitors.add(Monitors.wrap(tags, m));
    }
    return Collections.unmodifiableList(wrappedMonitors);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue(int pollerIdx) {
    return monitor.getValue(pollerIdx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof CompositeMonitorWrapper<?>)) {
      return false;
    }
    @SuppressWarnings("unchecked")
    CompositeMonitorWrapper<T> m = (CompositeMonitorWrapper<T>) obj;
    return config.equals(m.getConfig()) && monitor.equals(m.monitor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    result = 31 * result + monitor.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "CompositeMonitorWrapper{config=" + config + ", monitor=" + monitor + '}';
  }
}
