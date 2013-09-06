/**
 * Copyright 2013 Netflix, Inc.
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
import com.netflix.servo.Metric;
import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.TagList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Converts counter metrics into a rate per second. The rate is calculated by
 * comparing two samples of given metric and looking at the delta. Since two
 * samples are needed to calculate the rate, no value will be sent to the
 * wrapped observer until a second sample arrives. If a given metric is not
 * updated within a given heartbeat interval, the previous cached value for the
 * counter will be dropped such that if a new sample comes in it will be
 * treated as the first sample for that metric.
 *
 * <p>Counters should be monotonically increasing values. If a counter value
 * decreases from one sample to the next, then we will assume the counter value
 * was reset and send a rate of 0. This is similar to the RRD concept of
 * type DERIVE with a min of 0.
 *
 * <p>This class is not thread safe and should generally be wrapped by an async
 * observer to prevent issues.
 */
public final class CounterToRateMetricTransform implements MetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(CounterToRateMetricTransform.class);

    private static final String COUNTER_VALUE = DataSourceType.COUNTER.name();

    private final MetricObserver observer;
    private final Map<MonitorConfig, CounterValue> cache;

    private final long intervalMillis;

    /**
     * Creates a new instance with the specified heartbeat interval. The
     * heartbeat should be some multiple of the sampling interval used when
     * collecting the metrics.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "SE_BAD_FIELD_INNER_CLASS",
        justification = "We don't use serialization - ignore that LinkedHashMap is serializable")
    public CounterToRateMetricTransform(MetricObserver observer, long heartbeat, TimeUnit unit) {
        this(observer, heartbeat, 0L, unit);
    }

    /**
     * Creates a new instance with the specified heartbeat interval. The
     * heartbeat should be some multiple of the sampling interval used when
     * collecting the metrics.
     *
     * @param observer            downstream observer to forward values to after the rate has
     *                            been computed.
     * @param heartbeat           how long to remember a previous value before dropping it and
     *                            treating new samples as the first report.
     * @param estPollingInterval  estimated polling interval in to use for the first call. If set
     *                            to zero no values will be forwarded until the second sample for
     *                            a given counter. The delta for the first interval will be the
     *                            total value for the counter as it is assumed it started at 0 and
     *                            was first created since the last polling interval. If this
     *                            assumption is not true then this setting should be 0 so it waits
     *                            for the next sample to compute an accurate delta, otherwise
     *                            spikes will occur in the output.
     * @param unit                unit for the heartbeat and estPollingInterval params.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "SE_BAD_FIELD_INNER_CLASS",
        justification = "We don't use serialization - ignore that LinkedHashMap is serializable")
    public CounterToRateMetricTransform(
            MetricObserver observer, long heartbeat, long estPollingInterval, TimeUnit unit) {
        this.observer = observer;
        this.intervalMillis = TimeUnit.MILLISECONDS.convert(estPollingInterval, unit);

        final long heartbeatMillis = TimeUnit.MILLISECONDS.convert(heartbeat, unit);
        this.cache = new LinkedHashMap<MonitorConfig, CounterValue>(16, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<MonitorConfig, CounterValue> eldest) {
                final long now = System.currentTimeMillis();
                final long lastMod = eldest.getValue().getTimestamp();
                final boolean expired = (now - lastMod > heartbeatMillis);
                if (expired) {
                    LOGGER.debug("heartbeat interval exceeded, expiring {}", eldest.getKey());
                }
                return expired;
            }
        };
    }

    /** {@inheritDoc} */
    public String getName() {
        return observer.getName();
    }

    /** {@inheritDoc} */
    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        LOGGER.debug("received {} metrics", metrics.size());
        final List<Metric> newMetrics = Lists.newArrayListWithCapacity(metrics.size());
        for (Metric m : metrics) {
            if (isCounter(m)) {
                final CounterValue prev = cache.get(m.getConfig());
                if (prev != null) {
                    final double rate = prev.computeRate(m);
                    newMetrics.add(new Metric(m.getConfig(), m.getTimestamp(), rate));
                } else {
                    CounterValue current = new CounterValue(m);
                    cache.put(m.getConfig(), current);
                    if (intervalMillis > 0L) {
                        final double delta = m.getNumberValue().doubleValue();
                        final double rate = current.computeRate(intervalMillis, delta);
                        newMetrics.add(new Metric(m.getConfig(), m.getTimestamp(), rate));
                    }
                }
            } else {
                newMetrics.add(m);
            }
        }
        LOGGER.debug("writing {} metrics to downstream observer", newMetrics.size());
        observer.update(newMetrics);
    }

    /**
     * Clear all cached state of previous counter values.
     */
    public void reset() {
        cache.clear();
    }

    private boolean isCounter(Metric m) {
        final TagList tags = m.getConfig().getTags();
        final String value = tags.getValue(DataSourceType.KEY);
        return value != null && COUNTER_VALUE.equals(value);
    }

    private static class CounterValue {
        private long timestamp;
        private double value;

        public CounterValue(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public CounterValue(Metric m) {
            this(m.getTimestamp(), m.getNumberValue().doubleValue());
        }

        public long getTimestamp() {
            return timestamp;
        }

        public double computeRate(Metric m) {
            final long currentTimestamp = m.getTimestamp();
            final double currentValue = m.getNumberValue().doubleValue();

            final long durationMillis = currentTimestamp - timestamp;
            final double delta = currentValue - value;

            timestamp = currentTimestamp;
            value = currentValue;

            return computeRate(durationMillis, delta);
        }

        public double computeRate(long durationMillis, double delta) {
            final double millisPerSecond = 1000.0;
            final double duration = durationMillis / millisPerSecond;
            return (duration <= 0.0 || delta <= 0.0) ? 0.0 : delta / duration;
        }
    }
}
