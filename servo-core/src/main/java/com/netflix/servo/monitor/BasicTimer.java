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

import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;
import com.netflix.servo.util.UnmodifiableList;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * A simple timer implementation providing the total time, count, min, and max for the times that
 * have been recorded.
 */
public class BasicTimer extends AbstractMonitor<Long> implements Timer, CompositeMonitor<Long> {

  private static final String STATISTIC = "statistic";
  private static final String UNIT = "unit";

  private static final Tag STAT_TOTAL = Tags.newTag(STATISTIC, "totalTime");
  private static final Tag STAT_COUNT = Tags.newTag(STATISTIC, "count");
  private static final Tag STAT_TOTAL_SQ = Tags.newTag(STATISTIC, "totalOfSquares");
  private static final Tag STAT_MAX = Tags.newTag(STATISTIC, "max");

  private final TimeUnit timeUnit;
  private final double timeUnitNanosFactor;
  private final StepCounter totalTime;
  private final StepCounter count;
  private final DoubleCounter totalOfSquares;
  private final MaxGauge max;

  private final List<Monitor<?>> monitors;

  private static final class FactorMonitor<T extends Number> extends AbstractMonitor<Double>
      implements NumericMonitor<Double> {
    private final Monitor<T> wrapped;
    private final double factor;

    private FactorMonitor(Monitor<T> wrapped, double factor) {
      super(wrapped.getConfig());
      this.wrapped = wrapped;
      this.factor = factor;
    }

    @Override
    public Double getValue(int pollerIndex) {
      return wrapped.getValue(pollerIndex).doubleValue() * factor;
    }
  }

  /**
   * Creates a new instance of the timer with a unit of milliseconds.
   */
  public BasicTimer(MonitorConfig config) {
    this(config, TimeUnit.MILLISECONDS);
  }

  /**
   * Creates a new instance of the timer.
   */
  BasicTimer(MonitorConfig config, TimeUnit unit, Clock clock) {
    super(config);

    final Tag unitTag = Tags.newTag(UNIT, unit.name());
    final MonitorConfig unitConfig = config.withAdditionalTag(unitTag);
    timeUnit = unit;
    timeUnitNanosFactor = 1.0 / timeUnit.toNanos(1);

    totalTime = new StepCounter(unitConfig.withAdditionalTag(STAT_TOTAL), clock);
    count = new StepCounter(unitConfig.withAdditionalTag(STAT_COUNT), clock);
    totalOfSquares = new DoubleCounter(unitConfig.withAdditionalTag(STAT_TOTAL_SQ), clock);
    max = new MaxGauge(unitConfig.withAdditionalTag(STAT_MAX), clock);

    final FactorMonitor<Number> totalTimeFactor = new FactorMonitor<>(totalTime,
        timeUnitNanosFactor);
    final FactorMonitor<Number> totalSquaresFactor = new FactorMonitor<>(totalOfSquares,
        timeUnitNanosFactor * timeUnitNanosFactor);
    final FactorMonitor<Long> maxFactor = new FactorMonitor<>(max,
        timeUnitNanosFactor);

    monitors = UnmodifiableList.<Monitor<?>>of(totalTimeFactor, count,
        totalSquaresFactor, maxFactor);
  }

  /**
   * Creates a new instance of the timer.
   */
  public BasicTimer(MonitorConfig config, TimeUnit unit) {
    this(config, unit, ClockWithOffset.INSTANCE);
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

  private void recordNanos(long nanos) {
    if (nanos >= 0) {
      totalTime.increment(nanos);
      count.increment();
      totalOfSquares.increment((double) nanos * (double) nanos);
      max.update(nanos);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated
  public void record(long duration) {
    long nanos = timeUnit.toNanos(duration);
    recordNanos(nanos);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void record(long duration, TimeUnit unit) {
    recordNanos(unit.toNanos(duration));
  }

  private double getTotal(int pollerIndex) {
    return totalTime.getCurrentCount(pollerIndex) * timeUnitNanosFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long getValue(int pollerIndex) {
    final long cnt = count.getCurrentCount(pollerIndex);
    final long value = (long) (getTotal(pollerIndex) / cnt);
    return (cnt == 0) ? 0L : value;
  }

  /**
   * Get the total time for all updates.
   */
  public Double getTotalTime() {
    return getTotal(0);
  }

  /**
   * Get the total number of updates.
   */
  public Long getCount() {
    return count.getCurrentCount(0);
  }

  /**
   * Get the max value since the last reset.
   */
  public Double getMax() {
    return max.getCurrentValue(0) * timeUnitNanosFactor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || !(obj instanceof BasicTimer)) {
      return false;
    }
    BasicTimer m = (BasicTimer) obj;
    return config.equals(m.getConfig())
        && totalTime.equals(m.totalTime)
        && count.equals(m.count)
        && totalOfSquares.equals(m.totalOfSquares)
        && max.equals(m.max);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = config.hashCode();
    result = 31 * result + totalTime.hashCode();
    result = 31 * result + count.hashCode();
    result = 31 * result + totalOfSquares.hashCode();
    result = 31 * result + max.hashCode();
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BasicTimer{config=" + config + ", totalTime=" + totalTime
        + ", count=" + count + ", totalOfSquares=" + totalOfSquares
        + ", max=" + max + '}';
  }
}
