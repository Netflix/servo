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

import com.netflix.servo.stats.StatsBuffer;
import com.netflix.servo.stats.StatsConfig;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ThreadFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * A {@link Timer} that provides statistics.
 * <p>
 * The statistics are collected periodically and are published according to the configuration
 * specified by the user using a {@link com.netflix.servo.stats.StatsConfig} object.
 */
public class StatsMonitor extends AbstractMonitor<Long> implements
    CompositeMonitor<Long>, NumericMonitor<Long> {

  protected static final ScheduledExecutorService DEFAULT_EXECUTOR;
  private static final long EXPIRE_AFTER_MS;

  static {
    final String className = StatsMonitor.class.getCanonicalName();
    final String expirationProp = className + ".expiration";
    final String expirationPropUnit = className + ".expirationUnit";
    final String expiration = System.getProperty(expirationProp, "15");
    final String expirationUnit = System.getProperty(expirationPropUnit, "MINUTES");
    final long expirationValue = Long.parseLong(expiration);
    final TimeUnit expirationUnitValue = TimeUnit.valueOf(expirationUnit);
    EXPIRE_AFTER_MS = expirationUnitValue.toMillis(expirationValue);

    final ThreadFactory threadFactory = ThreadFactories.withName("StatsMonitor-%d");
    final ScheduledThreadPoolExecutor poolExecutor =
        new ScheduledThreadPoolExecutor(1, threadFactory);
    poolExecutor.setRemoveOnCancelPolicy(true);
    DEFAULT_EXECUTOR = poolExecutor;
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(StatsMonitor.class);

  private final MonitorConfig baseConfig;
  protected final Counter count;
  protected final Counter totalMeasurement;
  private final List<Monitor<?>> monitors;

  private final List<GaugeWrapper> gaugeWrappers;
  private final Runnable startComputingAction;

  private final Object updateLock = new Object();
  private StatsBuffer cur;
  private StatsBuffer prev;

  private static final String STATISTIC = "statistic";
  private static final String PERCENTILE_FMT = "percentile_%.2f";
  private static final Tag STAT_COUNT = Tags.newTag(STATISTIC, "count");
  private static final Tag STAT_MIN = Tags.newTag(STATISTIC, "min");
  private static final Tag STAT_MAX = Tags.newTag(STATISTIC, "max");
  private static final Tag STAT_MEAN = Tags.newTag(STATISTIC, "avg");
  private static final Tag STAT_VARIANCE = Tags.newTag(STATISTIC, "variance");
  private static final Tag STAT_STDDEV = Tags.newTag(STATISTIC, "stdDev");

  private final Clock clock;
  private volatile long lastUsed;
  private final ScheduledExecutorService executor;
  private final StatsConfig statsConfig;
  private AtomicReference<ScheduledFuture<?>> myFutureRef = new AtomicReference<>();

  private interface GaugeWrapper {
    void update(StatsBuffer buffer);

    Monitor<?> getMonitor();
  }

  private abstract static class LongGaugeWrapper implements GaugeWrapper {
    protected final LongGauge gauge;

    protected LongGaugeWrapper(MonitorConfig config) {
      gauge = new LongGauge(config);
    }

    @Override
    public Monitor<?> getMonitor() {
      return gauge;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof LongGaugeWrapper)) {
        return false;
      }
      final LongGaugeWrapper that = (LongGaugeWrapper) o;
      return gauge.equals(that.gauge);
    }

    @Override
    public int hashCode() {
      return gauge.hashCode();
    }

    @Override
    public String toString() {
      return "LongGaugeWrapper{gauge=" + gauge + '}';
    }
  }

  private abstract static class DoubleGaugeWrapper implements GaugeWrapper {
    protected final DoubleGauge gauge;

    protected DoubleGaugeWrapper(MonitorConfig config) {
      gauge = new DoubleGauge(config);
    }

    @Override
    public Monitor<?> getMonitor() {
      return gauge;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof DoubleGaugeWrapper)) {
        return false;
      }
      final DoubleGaugeWrapper that = (DoubleGaugeWrapper) o;
      return gauge.equals(that.gauge);
    }

    @Override
    public int hashCode() {
      return gauge.hashCode();
    }

    @Override
    public String toString() {
      return "DoubleGaugeWrapper{gauge=" + gauge + '}';
    }
  }

  private static class MinStatGaugeWrapper extends LongGaugeWrapper {
    MinStatGaugeWrapper(MonitorConfig baseConfig) {
      super(baseConfig.withAdditionalTag(STAT_MIN));
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getMin());
    }
  }

  private static class MaxGaugeWrapper extends LongGaugeWrapper {
    MaxGaugeWrapper(MonitorConfig baseConfig) {
      super(baseConfig.withAdditionalTag(STAT_MAX));
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getMax());
    }
  }

  private static class MeanGaugeWrapper extends DoubleGaugeWrapper {
    MeanGaugeWrapper(MonitorConfig baseConfig) {
      super(baseConfig.withAdditionalTag(STAT_MEAN));
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getMean());
    }
  }

  private static class VarianceGaugeWrapper extends DoubleGaugeWrapper {
    VarianceGaugeWrapper(MonitorConfig baseConfig) {
      super(baseConfig.withAdditionalTag(STAT_VARIANCE));
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getVariance());
    }
  }

  private static class StdDevGaugeWrapper extends DoubleGaugeWrapper {
    StdDevGaugeWrapper(MonitorConfig baseConfig) {
      super(baseConfig.withAdditionalTag(STAT_STDDEV));
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getStdDev());
    }
  }

  private static class PercentileGaugeWrapper extends DoubleGaugeWrapper {
    private final double percentile;
    private final int index;

    private static Tag percentileTag(double percentile) {
      String percentileStr = String.format(PERCENTILE_FMT, percentile);
      if (percentileStr.endsWith(".00")) {
        percentileStr = percentileStr.substring(0, percentileStr.length() - 3);
      }

      return Tags.newTag(STATISTIC, percentileStr);
    }

    PercentileGaugeWrapper(MonitorConfig baseConfig, double percentile, int index) {
      super(baseConfig.withAdditionalTag(percentileTag(percentile)));
      this.percentile = percentile;
      this.index = index;
    }

    @Override
    public void update(StatsBuffer buffer) {
      gauge.set(buffer.getPercentileValueForIdx(index));
    }


    @Override
    public String toString() {
      return "PercentileGaugeWrapper{gauge=" + gauge + "percentile=" + percentile + '}';
    }
  }

  private List<Counter> getCounters(StatsConfig config) {
    final List<Counter> counters = new ArrayList<>();
    if (config.getPublishCount()) {
      counters.add(count);
    }
    if (config.getPublishTotal()) {
      counters.add(totalMeasurement);
    }
    return counters;
  }

  private List<GaugeWrapper> getGaugeWrappers(StatsConfig config) {
    final List<GaugeWrapper> wrappers = new ArrayList<>();

    if (config.getPublishMax()) {
      wrappers.add(new MaxGaugeWrapper(baseConfig));
    }
    if (config.getPublishMin()) {
      wrappers.add(new MinStatGaugeWrapper(baseConfig));
    }
    if (config.getPublishVariance()) {
      wrappers.add(new VarianceGaugeWrapper(baseConfig));
    }
    if (config.getPublishStdDev()) {
      wrappers.add(new StdDevGaugeWrapper(baseConfig));
    }
    if (config.getPublishMean()) {
      wrappers.add(new MeanGaugeWrapper(baseConfig));
    }

    final double[] percentiles = config.getPercentiles();
    for (int i = 0; i < percentiles.length; ++i) {
      wrappers.add(new PercentileGaugeWrapper(baseConfig, percentiles[i], i));
    }

    // do a sanity check to prevent duplicated monitor configurations
    final Set<MonitorConfig> seen = new HashSet<>();
    for (final GaugeWrapper wrapper : wrappers) {
      final MonitorConfig cfg = wrapper.getMonitor().getConfig();
      if (seen.contains(cfg)) {
        throw new IllegalArgumentException("Duplicated monitor configuration found: "
            + cfg);
      }
      seen.add(cfg);
    }

    return wrappers;
  }

  /**
   * Creates a new instance of the timer with a unit of milliseconds,
   * using the {@link ScheduledExecutorService} provided by the user,
   * and the default Clock.
   * To avoid memory leaks the ScheduledExecutorService
   * should have the policy to remove tasks from the work queue.
   * See {@link ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy(boolean)}
   */
  public StatsMonitor(final MonitorConfig config,
                      final StatsConfig statsConfig,
                      final ScheduledExecutorService executor,
                      final String totalTagName,
                      final boolean autoStart,
                      final Tag... additionalTags) {
    this(config, statsConfig, executor, totalTagName, autoStart, Clock.WALL, additionalTags);
  }

  /**
   * Creates a new instance of the timer with a unit of milliseconds,
   * using the {@link ScheduledExecutorService} provided by the user.
   * To avoid memory leaks the ScheduledExecutorService
   * should have the policy to remove tasks from the work queue.
   * See {@link ScheduledThreadPoolExecutor#setRemoveOnCancelPolicy(boolean)}
   */
  public StatsMonitor(final MonitorConfig config,
                      final StatsConfig statsConfig,
                      final ScheduledExecutorService executor,
                      final String totalTagName,
                      final boolean autoStart,
                      final Clock clock,
                      final Tag... additionalTags) {


    super(config);
    final Tag statsTotal = Tags.newTag(STATISTIC, totalTagName);
    this.baseConfig = config.withAdditionalTags(new BasicTagList(Arrays.asList(additionalTags)));
    this.clock = clock;
    this.lastUsed = clock.now();
    this.executor = executor;
    this.statsConfig = statsConfig;
    this.cur = new StatsBuffer(statsConfig.getSampleSize(), statsConfig.getPercentiles());
    this.prev = new StatsBuffer(statsConfig.getSampleSize(), statsConfig.getPercentiles());
    this.count = new BasicCounter(baseConfig.withAdditionalTag(STAT_COUNT));
    this.totalMeasurement = new BasicCounter(baseConfig.withAdditionalTag(statsTotal));
    this.gaugeWrappers = getGaugeWrappers(statsConfig);
    final List<Monitor<?>> gaugeMonitors = gaugeWrappers.stream()
        .map(GaugeWrapper::getMonitor).collect(Collectors.toList());

    List<Monitor<?>> monitorList = new ArrayList<>();
    monitorList.addAll(getCounters(statsConfig));
    monitorList.addAll(gaugeMonitors);
    this.monitors = Collections.unmodifiableList(monitorList);

    this.startComputingAction = () ->
        startComputingStats(executor, statsConfig.getFrequencyMillis());
    if (autoStart) {
      startComputingStats();
    }
  }

  /**
   * starts computation.
   * Because of potential race conditions, derived classes may wish
   * to define initial state before calling this function which starts the executor
   */
  public void startComputingStats() {
    this.startComputingAction.run();
  }

  private void startComputingStats(ScheduledExecutorService executor, long frequencyMillis) {
    Runnable command = () -> {
      try {
        if (myFutureRef.get() == null) {
          return;
        }
        final boolean expired = (clock.now() - lastUsed) > EXPIRE_AFTER_MS;
        if (expired) {
          final ScheduledFuture<?> future = myFutureRef.getAndSet(null);
          if (future != null) {
            LOGGER.debug("Expiring unused StatsMonitor {}", getConfig().getName());
            future.cancel(true);
          }
          return;
        }

        synchronized (updateLock) {
          final StatsBuffer tmp = prev;
          prev = cur;
          cur = tmp;
        }
        prev.computeStats();
        updateGauges();
        prev.reset();
      } catch (Exception e) {
        handleException(e);
      }
    };

    this.myFutureRef.set(executor.scheduleWithFixedDelay(command, frequencyMillis, frequencyMillis,
        TimeUnit.MILLISECONDS));
  }

  private void updateGauges() {
    for (GaugeWrapper gauge : gaugeWrappers) {
      gauge.update(prev);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Monitor<?>> getMonitors() {
    lastUsed = clock.now();
    if (isExpired()) {
      LOGGER.info("Attempting to get the value for an expired monitor: {}."
              + "Will start computing stats again.",
          getConfig().getName());
      startComputingStats(executor, statsConfig.getFrequencyMillis());
      return Collections.emptyList();
    }
    return monitors;
  }

  /**
   * Record the measurement we want to perform statistics on.
   */
  public void record(long measurement) {
    synchronized (updateLock) {
      cur.record(measurement);
    }
    count.increment();
    totalMeasurement.increment(measurement);
  }

  /**
   * Get the value of the measurement.
   */
  @Override
  public Long getValue(int pollerIndex) {
    final long n = getCount(pollerIndex);
    return n > 0 ? totalMeasurement.getValue(pollerIndex).longValue() / n : 0L;
  }


  @Override
  public Long getValue() {
    return getValue(0);
  }

  /**
   * This is called when we encounter an exception while processing the values
   * recorded to compute the stats.
   *
   * @param e Exception encountered.
   */
  protected void handleException(Exception e) {
    LOGGER.warn("Unable to compute stats: ", e);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "StatsMonitor{baseConfig=" + baseConfig + ", monitors=" + monitors + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof StatsMonitor)) {
      return false;
    }

    final StatsMonitor m = (StatsMonitor) obj;
    return baseConfig.equals(m.baseConfig) && monitors.equals(m.monitors);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = baseConfig.hashCode();
    result = 31 * result + monitors.hashCode();
    return result;
  }

  /**
   * Get the number of times this timer has been updated.
   */
  public long getCount(int pollerIndex) {
    return count.getValue(pollerIndex).longValue();
  }


  /**
   * Get the total time recorded for this timer.
   */
  public long getTotalMeasurement() {
    return totalMeasurement.getValue().longValue();
  }

  /**
   * Whether the current monitor has expired, and its task removed from
   * the executor.
   */
  boolean isExpired() {
    return myFutureRef.get() == null;
  }
}
