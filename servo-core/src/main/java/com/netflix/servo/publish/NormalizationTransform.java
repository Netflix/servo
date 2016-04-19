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
package com.netflix.servo.publish;

import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;
import com.netflix.servo.util.Preconditions;
import com.netflix.servo.util.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Converts rate metrics into normalized values. See
 * <a href="http://www.vandenbogaerdt.nl/rrdtool/process.php">Rates, normalizing and
 * consolidating</a> for a
 * discussion on normalization of rates as done by rrdtool.
 */
public final class NormalizationTransform implements MetricObserver {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(NormalizationTransform.class);

  private static final String DEFAULT_DSTYPE = DataSourceType.RATE.name();

  static Counter newCounter(String name) {
    Counter c = Monitors.newCounter(name);
    DefaultMonitorRegistry.getInstance().register(c);
    return c;
  }

  @VisibleForTesting
  static final Counter HEARTBEAT_EXCEEDED = newCounter("servo.norm.heartbeatExceeded");

  private final MetricObserver observer;
  private final long heartbeatMillis;
  private final long stepMillis;
  private final Map<MonitorConfig, NormalizedValue> cache;

  /**
   * Creates a new instance with the specified sampling and heartbeat interval using the default
   * clock implementation.
   *
   * @param observer  downstream observer to forward values after rates have been normalized
   *                  to step boundaries
   * @param step      sampling interval in milliseconds
   * @param heartbeat how long to keep values before dropping them and treating new samples
   *                  as first report
   *                  (in milliseconds)
   * @deprecated Please use a constructor that specifies the the timeunit explicitly.
   */
  @Deprecated
  public NormalizationTransform(MetricObserver observer, long step, long heartbeat) {
    this(observer, step, heartbeat, TimeUnit.MILLISECONDS, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance with the specified sampling and heartbeat interval and the
   * specified clock implementation.
   *
   * @param observer  downstream observer to forward values after rates have been normalized
   *                  to step boundaries
   * @param step      sampling interval in milliseconds
   * @param heartbeat how long to keep values before dropping them and treating new samples as
   *                  first report (in milliseconds)
   * @param clock     The {@link com.netflix.servo.util.Clock} to use for getting
   *                  the current time.
   * @deprecated Please use a constructor that specifies the the timeunit explicitly.
   */
  @Deprecated
  public NormalizationTransform(MetricObserver observer, long step, final long heartbeat,
                                final Clock clock) {
    this(observer, step, heartbeat, TimeUnit.MILLISECONDS, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance with the specified sampling and heartbeat interval and the
   * specified clock implementation.
   *
   * @param observer  downstream observer to forward values after rates have been normalized
   *                  to step boundaries
   * @param step      sampling interval
   * @param heartbeat how long to keep values before dropping them and treating new samples
   *                  as first report
   * @param unit      {@link java.util.concurrent.TimeUnit} in which step and heartbeat
   *                  are specified.
   */
  public NormalizationTransform(MetricObserver observer, long step, long heartbeat,
                                TimeUnit unit) {
    this(observer, step, heartbeat, unit, ClockWithOffset.INSTANCE);
  }

  /**
   * Creates a new instance with the specified sampling and heartbeat interval and the specified
   * clock implementation.
   *
   * @param observer  downstream observer to forward values after rates have been normalized
   *                  to step boundaries
   * @param step      sampling interval
   * @param heartbeat how long to keep values before dropping them and treating new samples
   *                  as first report
   * @param unit      The {@link java.util.concurrent.TimeUnit} in which step
   *                  and heartbeat are specified.
   * @param clock     The {@link com.netflix.servo.util.Clock}
   *                  to use for getting the current time.
   */
  public NormalizationTransform(MetricObserver observer, long step, final long heartbeat,
                                TimeUnit unit, final Clock clock) {
    this.observer = Preconditions.checkNotNull(observer, "observer");
    Preconditions.checkArgument(step > 0, "step must be positive");
    this.stepMillis = unit.toMillis(step);
    Preconditions.checkArgument(heartbeat > 0, "heartbeat must be positive");
    this.heartbeatMillis = unit.toMillis(heartbeat);

    this.cache = new LinkedHashMap<MonitorConfig, NormalizedValue>(16, 0.75f, true) {
      protected boolean removeEldestEntry(Map.Entry<MonitorConfig, NormalizedValue> eldest) {
        final long lastMod = eldest.getValue().lastUpdateTime;
        if (lastMod < 0) {
          return false;
        }

        final long now = clock.now();
        final boolean expired = (now - lastMod) > heartbeatMillis;
        if (expired) {
          HEARTBEAT_EXCEEDED.increment();
          LOGGER.debug("heartbeat interval exceeded, expiring {}", eldest.getKey());
        }
        return expired;
      }
    };
  }

  private static String getDataSourceType(Metric m) {
    final TagList tags = m.getConfig().getTags();
    final String value = tags.getValue(DataSourceType.KEY);
    if (value != null) {
      return value;
    } else {
      return DEFAULT_DSTYPE;
    }
  }

  private static boolean isGauge(String dsType) {
    return dsType.equals(DataSourceType.GAUGE.name());
  }

  private static boolean isRate(String dsType) {
    return dsType.equals(DataSourceType.RATE.name());
  }

  private static boolean isNormalized(String dsType) {
    return dsType.equals(DataSourceType.NORMALIZED.name());
  }

  private static boolean isInformational(String dsType) {
    return dsType.equals(DataSourceType.INFORMATIONAL.name());
  }

  private Metric normalize(Metric m, long stepBoundary) {
    NormalizedValue normalizedValue = cache.get(m.getConfig());
    if (normalizedValue == null) {
      normalizedValue = new NormalizedValue();
      cache.put(m.getConfig(), normalizedValue);
    }

    double value = normalizedValue.updateAndGet(m.getTimestamp(),
        m.getNumberValue().doubleValue());
    return new Metric(m.getConfig(), stepBoundary, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void update(List<Metric> metrics) {
    Preconditions.checkNotNull(metrics, "metrics");
    final List<Metric> newMetrics = new ArrayList<>(metrics.size());

    for (Metric m : metrics) {
      long offset = m.getTimestamp() % stepMillis;
      long stepBoundary = m.getTimestamp() - offset;
      String dsType = getDataSourceType(m);
      if (isGauge(dsType) || isNormalized(dsType)) {
        Metric atStepBoundary = new Metric(m.getConfig(), stepBoundary, m.getValue());
        newMetrics.add(atStepBoundary); // gauges are not normalized
      } else if (isRate(dsType)) {
        Metric normalized = normalize(m, stepBoundary);
        if (normalized != null) {
          newMetrics.add(normalized);
        }
      } else if (!isInformational(dsType)) {
        // unknown type - use a safe fallback
        newMetrics.add(m); // we cannot normalize this
      }
    }
    observer.update(newMetrics);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return observer.getName();
  }

  private static final long NO_PREVIOUS_UPDATE = -1L;

  private class NormalizedValue {
    private long lastUpdateTime = NO_PREVIOUS_UPDATE;
    private double lastValue = 0.0;

    private double weightedValue(long offset, double value) {
      double weight = (double) offset / stepMillis;
      return value * weight;
    }

    double updateAndGet(long timestamp, double value) {
      double result = Double.NaN;
      if (timestamp > lastUpdateTime) {
        if (lastUpdateTime > 0 && timestamp - lastUpdateTime > heartbeatMillis) {
          lastUpdateTime = NO_PREVIOUS_UPDATE;
          lastValue = 0.0;
        }

        long offset = timestamp % stepMillis;
        long stepBoundary = timestamp - offset;

        if (lastUpdateTime < stepBoundary) {
          if (lastUpdateTime != NO_PREVIOUS_UPDATE) {
            long intervalOffset = lastUpdateTime % stepMillis;
            lastValue += weightedValue(stepMillis - intervalOffset, value);
            result = lastValue;
          } else if (offset == 0) {
            result = value;
          } else {
            result = weightedValue(stepMillis - offset, value);
          }

          lastValue = weightedValue(offset, value);
        } else {
          // Didn't cross step boundary, so update is more frequent than step
          // and we just need to
          // add in the weighted value
          long intervalOffset = timestamp - lastUpdateTime;
          lastValue += weightedValue(intervalOffset, value);
          result = lastValue;
        }
      }

      lastUpdateTime = timestamp;
      return result;
    }
  }
}
