/**
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

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.Monitors;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Clock;
import com.netflix.servo.util.ClockWithOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NormalizationTransform implements MetricObserver {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(NormalizationTransform.class);
    static Counter newCounter(String name) {
        Counter c = Monitors.newCounter(name);
        DefaultMonitorRegistry.getInstance().register(c);
        return c;
    }
    private static final String DEFAULT_DSTYPE = DataSourceType.RATE.name();
    private static final Counter heartbeatExpireCount = newCounter("servo.monitor.norm.heartbeatExpireCount");

    private final MetricObserver observer;
    private final long heartbeat;
    private final long step;
    private final Map<MonitorConfig, NormalizedValue> cache;

    public NormalizationTransform(MetricObserver observer, long step, long heartbeat) {
        this(observer, step, heartbeat, ClockWithOffset.INSTANCE);
    }

    public NormalizationTransform(MetricObserver observer, long step, final long heartbeat, final Clock clock) {
        Preconditions.checkNotNull(observer);
        this.observer = observer;
        Preconditions.checkArgument(step > 0, "step must be positive");
        this.step = step;
        Preconditions.checkArgument(heartbeat > 0, "heartbeat must be positive");
        this.heartbeat = heartbeat;

        this.cache = new LinkedHashMap<MonitorConfig, NormalizedValue>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<MonitorConfig, NormalizedValue> eldest) {
                final long now = clock.now();
                final long lastMod = eldest.getValue().lastUpdateTime;
                final boolean expired = (now - lastMod) > heartbeat;
                if (expired) {
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

    private static boolean isInformational(String dsType) {
        return dsType.equals(DataSourceType.INFORMATIONAL.name());
    }

    private MonitorConfig toGaugeConfig(MonitorConfig config) {
        return config.withAdditionalTag(DataSourceType.GAUGE);
    }

    private Metric normalize(Metric m, long stepBoundary) {
        NormalizedValue normalizedValue = cache.get(m.getConfig());
        if (normalizedValue == null) {
            normalizedValue = new NormalizedValue();
            cache.put(m.getConfig(), normalizedValue);
        }

        double value = normalizedValue.updateAndGet(m.getTimestamp(), m.getNumberValue().doubleValue());
        return new Metric(toGaugeConfig(m.getConfig()), stepBoundary, value);
    }

    /** {@inheritDoc} */
    @Override
    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        final List<Metric> newMetrics = Lists.newArrayListWithCapacity(metrics.size());

        for (Metric m : metrics) {
            long offset = m.getTimestamp() % step;
            long stepBoundary = m.getTimestamp() - offset;
            String dsType = getDataSourceType(m);
            if (isGauge(dsType)) {
                Metric atStepBoundary = new Metric(m.getConfig(), stepBoundary, m.getValue());
                newMetrics.add(atStepBoundary); // gauges are not normalized
            } else if (isRate(dsType)) {
                Metric normalized = normalize(m, stepBoundary);
                if (normalized != null) {
                    newMetrics.add(normalized);
                }
            } else if (!isInformational(dsType)) {
                // TODO how to deal with this error in configuration
                LOGGER.warn("NormalizationTransform should get only GAUGE and RATE metrics. Please use CounterToRateMetricTransform. "
                                + m.getConfig()
                );
            }
        }
        observer.update(newMetrics);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return observer.getName();
    }

    private static long NO_PREVIOUS_UPDATE = -1L;
    private class NormalizedValue {
        long lastUpdateTime = NO_PREVIOUS_UPDATE;
        double lastValue = 0.0;

        private double weightedValue(long offset, double value) {
            double weight = (double) offset / step;
            return value * weight;
        }

        double updateAndGet(long timestamp, double value) {
            double result = Double.NaN;
            if (timestamp > lastUpdateTime) {
                if (lastUpdateTime > 0 && timestamp - lastUpdateTime > heartbeat) {
                   heartbeatExpireCount.increment();
                    lastUpdateTime = NO_PREVIOUS_UPDATE;
                    lastValue = 0.0;
                }

                long offset = timestamp % step;
                long stepBoundary = timestamp - offset;

                if (lastUpdateTime < stepBoundary) {
                    if (lastUpdateTime != NO_PREVIOUS_UPDATE) {
                        long intervalOffset = lastUpdateTime % step;
                        lastValue += weightedValue(step - intervalOffset, value);
                        result = lastValue;
                    } else if (offset == 0) {
                        result = value;
                    } else {
                        result = weightedValue(step - offset, value);
                    }

                    lastValue = weightedValue(offset, value);
                } else {
                    // Didn't cross step boundary, so update is more frequent than step and we just need to
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
