/**
 * Copyright 2015 Netflix, Inc.
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
import com.netflix.servo.util.VisibleForTesting;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A simple counter implementation backed by a StepLong but using doubles.
 * The value returned is a rate for the
 * previous interval as defined by the step.
 */
class DoubleCounter extends AbstractMonitor<Number> implements NumericMonitor<Number> {

  private final StepLong count;

  /**
   * Creates a new instance of the counter.
   */
  DoubleCounter(MonitorConfig config, Clock clock) {
    // This class will reset the value so it is not a monotonically increasing value as
    // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
    // the publishing pipeline receiving the value.
    super(config.withAdditionalTag(DataSourceType.NORMALIZED));
    count = new StepLong(0L, clock);
  }

  private void add(AtomicLong num, double amount) {
    long v;
    double d;
    long next;
    do {
      v = num.get();
      d = Double.longBitsToDouble(v);
      next = Double.doubleToLongBits(d + amount);
    } while (!num.compareAndSet(v, next));
  }

  /**
   * Increment the value by the specified amount.
   */
  void increment(double amount) {
    if (amount >= 0.0) {
      for (int i = 0; i < Pollers.NUM_POLLERS; ++i) {
        add(count.getCurrent(i), amount);
      }
    }
  }

  @Override
  public Number getValue(int pollerIndex) {
    final long n = count.poll(pollerIndex);
    final double stepSeconds = Pollers.POLLING_INTERVALS[pollerIndex] / 1000.0;
    return Double.longBitsToDouble(n) / stepSeconds;
  }

  /**
   * Get the current count for the given poller index.
   */
  @VisibleForTesting
  public double getCurrentCount(int pollerIndex) {
    return Double.longBitsToDouble(count.getCurrent(pollerIndex).get());
  }

  @Override
  public String toString() {
    return "DoubleCounter{"
        + "config=" + config
        + "count=" + Double.longBitsToDouble(count.getCurrent(0).longValue())
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DoubleCounter that = (DoubleCounter) o;
    return config.equals(that.config) && Objects.equals(getValue(0), that.getValue(0));
  }

  @Override
  public int hashCode() {
    return Objects.hash(config, getValue(0));
  }
}
