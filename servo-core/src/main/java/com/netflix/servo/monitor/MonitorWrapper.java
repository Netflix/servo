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

import com.netflix.servo.tag.TagList;

/**
 * Wraps another monitor object providing an alternative configuration.
 */
class MonitorWrapper<T> extends AbstractMonitor<T> {

  @SuppressWarnings("unchecked")
  static <T> MonitorWrapper<T> create(TagList tags, Monitor<T> monitor) {
    if (monitor instanceof NumericMonitor<?>) {
      return (MonitorWrapper<T>) ((monitor instanceof SpectatorMonitor)
          ? new SpectatorMonitorWrapper(tags, (NumericMonitor<?>) monitor)
          : new NumericMonitorWrapper(tags, (NumericMonitor<?>) monitor));
    } else {
      return new MonitorWrapper<>(tags, monitor);
    }
  }

  private final Monitor<T> monitor;

  /**
   * Creates a new instance of the wrapper.
   */
  MonitorWrapper(TagList tags, Monitor<T> monitor) {
    super(monitor.getConfig().withAdditionalTags(tags));
    this.monitor = monitor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T getValue(int pollerIdx) {
    return monitor.getValue();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof MonitorWrapper<?>)) {
      return false;
    }
    MonitorWrapper m = (MonitorWrapper) obj;
    return config.equals(m.getConfig()) && monitor.equals(m.monitor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = getConfig().hashCode();
    result = 31 * result + monitor.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "MonitorWrapper{config=" + config + ", monitor=" + monitor + '}';
  }
}
