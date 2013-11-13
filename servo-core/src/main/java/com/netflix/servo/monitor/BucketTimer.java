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
package com.netflix.servo.monitor;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * A simple timer implementation providing the total time, count, min, and max for the times that
 * have been recorded.
 */
public class BucketTimer extends AbstractMonitor<Long> implements Timer, CompositeMonitor<Long> {

    private static final String BUCKET = "bucket";
    private static final String UNIT = "unit";

    private static final Tag BUCKET_TOTAL = Tags.newTag(BUCKET, "totalTime");
    private static final Tag BUCKET_COUNT = Tags.newTag(BUCKET, "count");
    private static final Tag BUCKET_MIN = Tags.newTag(BUCKET, "min");
    private static final Tag BUCKET_MAX = Tags.newTag(BUCKET, "max");

    private final TimeUnit timeUnit;

    private final Counter totalTime;
    private final Counter count;
    private final MinGauge min;
    private final MaxGauge max;

    private final Counter[] bucketTime;
    private final Counter[] bucketCount;

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
        super(config);
        this.bucketConfig = Preconditions.checkNotNull(bucketConfig, "bucketConfig");

        final Tag unitTag = Tags.newTag(UNIT, unit.name());
        final MonitorConfig unitConfig = config.withAdditionalTag(unitTag);
        this.timeUnit = unit;

        this.totalTime = new BasicCounter(unitConfig.withAdditionalTag(BUCKET_TOTAL));
        this.count = new BasicCounter(unitConfig.withAdditionalTag(BUCKET_COUNT));
        this.min = new MinGauge(unitConfig.withAdditionalTag(BUCKET_MIN));
        this.max = new MaxGauge(unitConfig.withAdditionalTag(BUCKET_MAX));

        final long[] buckets = bucketConfig.getBuckets();
        final int numBuckets = buckets.length;
        this.bucketTime = new Counter[numBuckets];
        this.bucketCount = new Counter[numBuckets];
        for (int i = 0; i < numBuckets; i++) {
            String t = (bucketConfig.isReverseCumulative() ? "RevCum_" : "_")
                     + buckets[i]
                     + bucketConfig.getTimeUnitAbbreviation();
            bucketTime[i] = new BasicCounter(unitConfig
                .withAdditionalTag(Tags.newTag(BUCKET, "bucketTime" + t))
            );
            bucketCount[i] = new BasicCounter(unitConfig
                .withAdditionalTag(Tags.newTag(BUCKET, "bucketCount" + t))
            );
        }

        this.monitors = new ImmutableList.Builder<Monitor<?>>()
            .add(totalTime)
            .add(count)
            .add(min)
            .add(max)
            .addAll(Arrays.asList(bucketTime))
            .addAll(Arrays.asList(bucketCount))
            .build();
    }

    /** {@inheritDoc} */
    @Override
    public List<Monitor<?>> getMonitors() {
        return monitors;
    }

    /** {@inheritDoc} */
    @Override
    public Stopwatch start() {
        Stopwatch s = new TimedStopwatch(this);
        s.start();
        return s;
    }

    /** {@inheritDoc} */
    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration) {
        totalTime.increment(duration);
        count.increment();
        min.update(duration);
        max.update(duration);

        final long[] buckets = bucketConfig.getBuckets();
        for (int i = buckets.length - 1; i >= 0; i--) {
            if (buckets[i] <= duration) {
                if (bucketConfig.isReverseCumulative()) {
                    for (int j = 0; j < i; j++) {
                        bucketTime[j].increment(duration);
                        bucketCount[j].increment();
                    }
                }
                bucketTime[i].increment(duration);
                bucketCount[i].increment();
                break;
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void record(long duration, TimeUnit unit) {
        record(this.timeUnit.convert(duration, unit));
    }

    /** {@inheritDoc} */
    @Override
    public Long getValue() {
        final long cnt = count.getValue().longValue();
        return (cnt == 0) ? 0L : totalTime.getValue().longValue() / cnt;
    }

    /** Get the total time for all updates. */
    public Long getTotalTime() {
        return totalTime.getValue().longValue();
    }

    /** Get the total number of updates. */
    public Long getCount() {
        return count.getValue().longValue();
    }

    /** Get the min value since the last reset. */
    public Long getMin() {
        return min.getValue();
    }

    /** Get the max value since the last reset. */
    public Long getMax() {
        return max.getValue();
    }

    /** {@inheritDoc} */
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
            && count.equals(m.count)
            && min.equals(m.min)
            && max.equals(m.max)
            && Arrays.equals(bucketTime, m.bucketTime)
            && Arrays.equals(bucketCount, m.bucketCount);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hashCode(
            config,
            bucketConfig,
            timeUnit,
            totalTime,
            count,
            min,
            max,
            Arrays.hashCode(bucketTime),
            Arrays.hashCode(bucketCount)
        );
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("config", config)
                .add("bucketConfig", bucketConfig)
                .add("timeUnit", timeUnit)
                .add("totalTime", totalTime)
                .add("count", count)
                .add("min", min)
                .add("max", max)
                .add("bucketTime", bucketTime)
                .add("bucketCount", bucketCount)
                .toString();
    }
}
