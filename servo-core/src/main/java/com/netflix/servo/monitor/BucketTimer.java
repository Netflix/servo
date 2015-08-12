/*
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;
import com.netflix.servo.util.Preconditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * A timer implementation using a bucketing approach
 * that provides a way to get the distribution of samples if the primary
 * range of values is known.
 * <p/>
 * For example the following code:
 * <p/>
 * <pre>
 * BucketTimer t = new BucketTimer(
 *    MonitorConfig.builder(name).build(),
 *    new BucketConfig.Builder().withBuckets(new long[]{10L, 20L}).build());
 * </pre>
 * <p/>
 * will create a <code>BucketTimer</code> that in addition to the statistics:
 * <code>totalTime</code>, <code>min</code>, <code>max </code> will report:
 * <ul>
 * <li>statistic=count bucket=10ms</li>
 * <li>statistic=count bucket=20ms</li>
 * <li>statistic=count bucket=overflow</li>
 * </ul>
 * <p/>
 * <ul>
 * <li>bucket=10ms will contain the number of samples that were recorded that were
 * lower or equal to 10.</li>
 * <p/>
 * <li>bucket=20ms will contain the number of samples where the value was lower or equal to 20ms
 * and higher than 10.</li>
 * <p/>
 * <li>bucket=overflow will contain the remaining entries.</li>
 * </ul>
 * Please note that there are no default pre-configured buckets since it is highly dependant
 * on the use-case. If you fail to specify buckets in {@link BucketConfig} you will get a NPE.
 */

public class BucketTimer extends AbstractMonitor<Long> implements Timer, CompositeMonitor<Long> {

  private static final String STATISTIC = "statistic";
  private static final String BUCKET = "servo.bucket";
  private static final String UNIT = "unit";

  private static final Tag STAT_TOTAL = Tags.newTag(STATISTIC, "totalTime");
  private static final Tag STAT_COUNT = Tags.newTag(STATISTIC, "count");
  private static final Tag STAT_MIN = Tags.newTag(STATISTIC, "min");
  private static final Tag STAT_MAX = Tags.newTag(STATISTIC, "max");

  private final TimeUnit timeUnit;

  private final Counter totalTime;
  private final Counter[] bucketCount;
  private final Counter overflowCount;

  private final MinGauge min;
  private final MaxGauge max;

  private final List<Monitor<?>> monitors;
  private final BucketConfig bucketConfig;

  /**
   * Creates a new instance of the timer with a unit of milliseconds.
   */
  public BucketTimer(MonitorConfig config, BucketConfig bucketConfig) {
    this(config, bucketConfig, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates a new instance of the timer.
   */
  public BucketTimer(MonitorConfig config, BucketConfig bucketConfig, TimeUnit unit) {
    this(config, bucketConfig, unit, ClockWithOffset.INSTANCE);
  }

  BucketTimer(MonitorConfig config, BucketConfig bucketConfig, TimeUnit unit, Clock clock) {
    super(config);
    this.bucketConfig = Preconditions.checkNotNull(bucketConfig, "bucketConfig");

    final Tag unitTag = Tags.newTag(UNIT, unit.name());
    final MonitorConfig unitConfig = config.withAdditionalTag(unitTag);
    this.timeUnit = unit;

    this.totalTime = new BasicCounter(unitConfig.withAdditionalTag(STAT_TOTAL));
    this.overflowCount = new BasicCounter(unitConfig
        .withAdditionalTag(STAT_COUNT)
        .withAdditionalTag(Tags.newTag(BUCKET, "bucket=overflow")));
    this.min = new MinGauge(unitConfig.withAdditionalTag(STAT_MIN), clock);
    this.max = new MaxGauge(unitConfig.withAdditionalTag(STAT_MAX), clock);

    final long[] buckets = bucketConfig.getBuckets();
    final int numBuckets = buckets.length;
    final int numDigits = Long.toString(buckets[numBuckets - 1]).length();
    final String label = bucketConfig.getTimeUnitAbbreviation();

    this.bucketCount = new Counter[numBuckets];

    for (int i = 0; i < numBuckets; i++) {
      bucketCount[i] = new BasicCounter(unitConfig
          .withAdditionalTag(STAT_COUNT)
          .withAdditionalTag(Tags.newTag(BUCKET,
              String.format("bucket=%0" + numDigits + "d%s", buckets[i], label)))
      );
    }

    List<Monitor<?>> monitorList = new ArrayList<>();
    monitorList.add(totalTime);
    monitorList.add(min);
    monitorList.add(max);
    monitorList.addAll(Arrays.asList(bucketCount));
    monitorList.add(overflowCount);
    this.monitors = Collections.unmodifiableList(monitorList);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    return monitors;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Stopwatch start() {
    Stopwatch s = new TimedStopwatch(this);
    s.start();
    return s;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void record(long duration) {
    totalTime.increment(duration);
    min.update(duration);
    max.update(duration);

    final long[] buckets = bucketConfig.getBuckets();
    final long bucketDuration = bucketConfig.getTimeUnit().convert(duration, timeUnit);
    for (int i = 0; i < buckets.length; i++) {
      if (bucketDuration <= buckets[i]) {
        bucketCount[i].increment();
        return;
      }
    }
    overflowCount.increment();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void record(long duration, TimeUnit unit) {
    record(this.timeUnit.convert(duration, unit));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIndex) {
    final long cnt = getCount(pollerIndex);
    return (cnt == 0) ? 0L : totalTime.getValue().longValue() / cnt;
  }

  /**
   * Get the total time for all updates.
   */
  public Long getTotalTime() {
    return totalTime.getValue().longValue();
  }

  /**
   * Get the total number of updates.
   */
  public Long getCount(int pollerIndex) {
    long updates = 0;
    for (Counter c : bucketCount) {
      updates += c.getValue(pollerIndex).longValue();
    }
    updates += overflowCount.getValue(pollerIndex).longValue();

    return updates;
  }

  /**
   * Get the min value since the last reset.
   */
  public Long getMin(int pollerIndex) {
    return min.getValue(pollerIndex);
  }

  /**
   * Get the max value since the last reset.
   */
  public Long getMax(int pollerIndex) {
    return max.getValue(pollerIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof BucketTimer)) {
      return false;
    }
    BucketTimer m = (BucketTimer) obj;
    return config.equals(m.getConfig())
        && bucketConfig.equals(m.bucketConfig)
        && timeUnit.equals(m.timeUnit)
        && totalTime.equals(m.totalTime)
        && min.equals(m.min)
        && max.equals(m.max)
        && overflowCount.equals(m.overflowCount)
        && Arrays.equals(bucketCount, m.bucketCount);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    result = 31 * result + timeUnit.hashCode();
    result = 31 * result + totalTime.hashCode();
    result = 31 * result + Arrays.hashCode(bucketCount);
    result = 31 * result + overflowCount.hashCode();
    result = 31 * result + min.hashCode();
    result = 31 * result + max.hashCode();
    result = 31 * result + bucketConfig.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BucketTimer{config=" + config
        + ", bucketConfig=" + bucketConfig
        + ", timeUnit=" + timeUnit
        + ", totalTime=" + totalTime
        + ", min=" + min
        + ", max=" + max
        + ", bucketCount=" + Arrays.toString(bucketCount)
        + ", overflowCount=" + overflowCount
        + '}';
  }
}
