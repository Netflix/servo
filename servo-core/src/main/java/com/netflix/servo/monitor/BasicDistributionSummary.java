/**
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import com.netflix.servo.util.UnmodifiableList;

import java.util.List;

/**
 * Track the sample distribution of events. Similar to a BasicTimer without the time unit aspect.
 */
public class BasicDistributionSummary
    extends AbstractMonitor<Long> implements CompositeMonitor<Long> {

  private static final String STATISTIC = "statistic";

  private static final Tag STAT_TOTAL = Tags.newTag(STATISTIC, "totalAmount");
  private static final Tag STAT_COUNT = Tags.newTag(STATISTIC, "count");
  private static final Tag STAT_MAX = Tags.newTag(STATISTIC, "max");
  private static final Tag STAT_MIN = Tags.newTag(STATISTIC, "min");

  private final StepCounter totalAmount;
  private final StepCounter count;
  private final MaxGauge max;
  private final MinGauge min;

  private final List<Monitor<?>> monitors;

  /**
   * Create a new instance.
   */
  public BasicDistributionSummary(MonitorConfig config) {
    super(config);

    totalAmount = new StepCounter(config.withAdditionalTag(STAT_TOTAL));
    count = new StepCounter(config.withAdditionalTag(STAT_COUNT));
    max = new MaxGauge(config.withAdditionalTag(STAT_MAX));
    min = new MinGauge(config.withAdditionalTag(STAT_MIN));

    monitors = UnmodifiableList.<Monitor<?>>of(totalAmount, count, max, min);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    return monitors;
  }

  /**
   * Updates the statistics kept by the summary with the specified amount.
   */
  public void record(long amount) {
    if (amount >= 0) {
      totalAmount.increment(amount);
      count.increment();
      max.update(amount);
      min.update(amount);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIndex) {
    final long cnt = count.getCurrentCount(pollerIndex);
    final long total = totalAmount.getCurrentCount(pollerIndex);
    final long value = (long) ((double) total / cnt);
    return (cnt == 0) ? 0L : value;
  }

  /**
   * Get the total amount for all updates.
   */
  public Long getTotalAmount() {
    return totalAmount.getCurrentCount(0);
  }

  /**
   * Get the total number of updates.
   */
  public Long getCount() {
    return count.getCurrentCount(0);
  }

  /**
   * Get the min value since the last polling interval.
   */
  public Long getMin() {
    return min.getCurrentValue(0);
  }

  /**
   * Get the max value since the last polling interval.
   */
  public Long getMax() {
    return max.getCurrentValue(0);
  }

  @Override
  public String toString() {
    return "BasicDistributionSummary{config=" + config
        + ", totalAmount=" + totalAmount
        + ", count=" + count
        + ", max=" + max
        + ", min=" + min
        + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    result = 31 * result + totalAmount.hashCode();
    result = 31 * result + count.hashCode();
    result = 31 * result + max.hashCode();
    result = 31 * result + min.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || !(obj instanceof BasicDistributionSummary)) {
      return false;
    }

    BasicDistributionSummary m = (BasicDistributionSummary) obj;
    return config.equals(m.getConfig())
        && totalAmount.equals(m.totalAmount)
        && count.equals(m.count)
        && max.equals(m.max)
        && min.equals(m.min);
  }
}
