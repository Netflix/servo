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
import com.netflix.servo.util.VisibleForTesting;

/**
 * A simple counter implementation backed by a StepLong. The value returned is a rate for the
 * previous interval as defined by the step.
 */
public class StepCounter extends AbstractMonitor<Number> implements Counter {

  private final StepLong count;

  /**
   * Creates a new instance of the counter.
   */
  public StepCounter(MonitorConfig config) {
    this(config, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance of the counter.
   */
  @VisibleForTesting
  public StepCounter(MonitorConfig config, Clock clock) {
    // This class will reset the value so it is not a monotonically increasing value as
    // expected for type=COUNTER. This class looks like a counter to the user and a gauge to
    // the publishing pipeline receiving the value.
    super(config.withAdditionalTag(DataSourceType.NORMALIZED));
    count = new StepLong(0L, clock);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment() {
    count.addAndGet(1L);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void increment(long amount) {
    if (amount > 0L) {
      count.addAndGet(amount);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Number getValue(int pollerIndex) {
    final long n = count.poll(pollerIndex);
    final double stepSeconds = Pollers.POLLING_INTERVALS[pollerIndex] / 1000.0;
    return n / stepSeconds;
  }

  /**
   * Get the count for the last completed polling interval for the given poller index.
   */
  public long getCount(int pollerIndex) {
    return count.poll(pollerIndex);
  }

  /**
   * Get the current count for the given poller index.
   */
  @VisibleForTesting
  public long getCurrentCount(int pollerIndex) {
    return count.getCurrent(pollerIndex).get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "StepCounter{config=" + config + ", count=" + count + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    StepCounter that = (StepCounter) o;
    return config.equals(that.config) && getCount(0) == that.getCount(0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return config.hashCode();
  }
}
