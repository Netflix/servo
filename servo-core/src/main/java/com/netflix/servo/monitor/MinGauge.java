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

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Gauge that keeps track of the minimum value seen since the last reset. The reset value is
 * Long.MAX_VALUE. If no update has been received since the last reset, then {@link #getValue}
 * will return 0.
 */
public class MinGauge extends AbstractMonitor<Long>
    implements Gauge<Long> {
  private final StepLong min;

  /**
   * Creates a new instance of the gauge.
   */
  public MinGauge(MonitorConfig config) {
    this(config, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance of the gauge using a specific Clock. Useful for unit testing.
   */
  MinGauge(MonitorConfig config, Clock clock) {
    super(config.withAdditionalTag(DataSourceType.GAUGE));
    min = new StepLong(Long.MAX_VALUE, clock);
  }

  private void updateMin(int idx, long v) {
    AtomicLong current = min.getCurrent(idx);
    long m = current.get();
    while (v < m) {
      if (current.compareAndSet(m, v)) {
        break;
      }
      m = current.get();
    }
  }

  /**
   * Update the min if the provided value is smaller than the current min.
   */
  public void update(long v) {
    for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
      updateMin(i, v);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIdx) {
    long v = min.poll(pollerIdx);
    return (v == Long.MAX_VALUE) ? 0L : v;
  }

  /**
   * Returns the current min value since the last reset.
   */
  public long getCurrentValue(int nth) {
    long v = min.getCurrent(nth).get();
    return (v == Long.MAX_VALUE) ? 0L : v;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof MinGauge)) {
      return false;
    }
    MinGauge m = (MinGauge) obj;
    return config.equals(m.getConfig()) && getValue(0).equals(m.getValue(0));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = getConfig().hashCode();
    result = 31 * result + getValue(0).hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "MinGauge{config=" + config + ", min=" + min + '}';
  }
}
