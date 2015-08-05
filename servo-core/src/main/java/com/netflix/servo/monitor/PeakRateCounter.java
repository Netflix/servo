/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The value is the maximum count per second within the specified interval.
 */
public class PeakRateCounter extends AbstractMonitor<Number>
    implements Counter {

  private final Clock clock;

  private final AtomicLong currentSecond = new AtomicLong();
  private final AtomicLong currentCount = new AtomicLong();

  /**
   * Creates a counter implementation that records the maximum count per second
   * within a specific interval.
   */
  public PeakRateCounter(MonitorConfig config) {
    this(config, ClockWithOffset.INSTANCE);
  }

  private final StepLong peakRate;

  PeakRateCounter(MonitorConfig config, Clock clock) {
    super(config.withAdditionalTag(DataSourceType.GAUGE));

    this.clock = clock;
    this.peakRate = new StepLong(0L, clock);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIdx) {
    return peakRate.getCurrent(pollerIdx);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || !(obj instanceof PeakRateCounter)) {
      return false;
    }
    final PeakRateCounter c = (PeakRateCounter) obj;
    final double v = getValue().doubleValue();
    final double otherV = c.getValue().doubleValue();
    return config.equals(c.getConfig())
        && Double.compare(v, otherV) == 0;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = getConfig().hashCode();
    final long n = Double.doubleToLongBits(getValue().doubleValue());
    result = 31 * result + (int) (n ^ (n >>> 32));
    return result;
  }

  /**
   * {@inheritDoc}
   */
  public String toString() {
    return "PeakRateCounter{config=" + config + ", max rate per second=" + getValue() + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment() {
    increment(1L);
  }

  private void updatePeakPoller(int idx, long v) {
    AtomicLong current = peakRate.getCurrent(idx);
    long m = current.get();
    while (v > m) {
      if (current.compareAndSet(m, v)) {
        break;
      }
      m = current.get();
    }
  }

  private void updatePeak(long v) {
    for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
      updatePeakPoller(i, v);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment(long amount) {
    long now = clock.now() / 1000L;
    if (now != currentSecond.get()) {
      currentCount.set(0);
      currentSecond.set(now);
    }
    long count = currentCount.addAndGet(amount);
    updatePeak(count);
  }
}

