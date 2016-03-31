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
 * Gauge that keeps track of the maximum value seen since the last reset. Updates should be
 * non-negative, the reset value is 0.
 */
public class MaxGauge extends AbstractMonitor<Long>
    implements Gauge<Long> {
  private final StepLong max;

  /**
   * Creates a new instance of the gauge.
   */
  public MaxGauge(MonitorConfig config) {
    this(config, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance of the gauge using a specific clock. Useful for unit testing.
   */
  MaxGauge(MonitorConfig config, Clock clock) {
    super(config.withAdditionalTag(DataSourceType.GAUGE));
    max = new StepLong(0L, clock);
  }

  /**
   * Update the max for the given index if the provided value is larger than the current max.
   */
  private void updateMax(int idx, long v) {
    AtomicLong current = max.getCurrent(idx);
    long m = current.get();
    while (v > m) {
      if (current.compareAndSet(m, v)) {
        break;
      }
      m = current.get();
    }
  }

  /**
   * Update the max if the provided value is larger than the current max.
   */
  public void update(long v) {
    for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
      updateMax(i, v);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int nth) {
    return max.poll(nth);
  }

  /**
   * Returns the current max value since the last reset.
   */
  public long getCurrentValue(int nth) {
    return max.getCurrent(nth).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof MaxGauge)) {
      return false;
    }
    MaxGauge m = (MaxGauge) obj;
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
    return "MaxGauge{config=" + config + ", max=" + max + '}';
  }
}
