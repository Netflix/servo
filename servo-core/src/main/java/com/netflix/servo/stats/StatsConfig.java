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
package com.netflix.servo.stats;

import com.google.common.base.Objects;

import java.util.Arrays;

/**
 * Configuration options for a {@link com.netflix.servo.monitor.StatsTimer}
 * <p>
 * By default we publish count (number of times the timer was executed), totalTime, and
 * 95.0, and 99.0 percentiles.
 * <p>
 * The size for the buffer used to store samples is controlled using the sampleSize field, and the frequency
 * at which stats are computed is controlled with the computeFrequencyMillis option. By default these are
 * set to 100,000 entries in the buffer, and computation at 60,000 ms (1 minute) intervals.
 *
 */
public final class StatsConfig {
    private static final String CLASS_NAME = StatsConfig.class.getCanonicalName();
    private static final String SIZE_PROP = CLASS_NAME + ".sampleSize";
    private static final String FREQ_PROP = CLASS_NAME + ".computeFreqMillis";

    /**
     * Builder for StatsConfig. By default the configuration includes count, total and 95th and 99th percentiles.
     */
    public static class Builder {
        private boolean publishCount = true;
        private boolean publishTotal = true;
        private boolean publishMin = false;
        private boolean publishMax = false;
        private boolean publishMean = false;
        private boolean publishVariance = false;
        private boolean publishStdDev = false;
        private int sampleSize = Integer.valueOf(System.getProperty(SIZE_PROP, "100000"));
        private long frequencyMillis = Long.valueOf(System.getProperty(FREQ_PROP, "60000"));

        private double[] percentiles = { 95.0, 99.0 };

        public Builder withPublishCount(boolean publishCount) {
            this.publishCount = publishCount;
            return this;
        }

        public Builder withPublishTotal(boolean publishTotal) {
            this.publishTotal = publishTotal;
            return this;
        }

        public Builder withPublishMin(boolean publishMin) {
            this.publishMin = publishMin;
            return this;
        }

        public Builder withPublishMax(boolean publishMax) {
            this.publishMax = publishMax;
            return this;
        }

        public Builder withPublishMean(boolean publishMean) {
            this.publishMean = publishMean;
            return this;
        }

        public Builder withPublishVariance(boolean publishVariance) {
            this.publishVariance = publishVariance;
            return this;
        }

        public Builder withPublishStdDev(boolean publishStdDev) {
            this.publishStdDev = publishStdDev;
            return this;
        }

        public Builder withPercentiles(double[] percentiles) {
            this.percentiles = Arrays.copyOf(percentiles, percentiles.length);
            return this;
        }

        public Builder withSampleSize(int size) {
            this.sampleSize = size;
            return this;
        }

        public Builder withComputeFrequencyMillis(long frequencyMillis) {
            this.frequencyMillis = frequencyMillis;
            return this;
        }

        public StatsConfig build() {
            return new StatsConfig(this);
        }
    }

    private final boolean publishCount;
    private final boolean publishTotal;
    private final boolean publishMin;
    private final boolean publishMax;
    private final boolean publishMean;
    private final boolean publishVariance;
    private final boolean publishStdDev;
    private final double[] percentiles;
    private final int sampleSize;
    private final long frequencyMillis;

    /**
     * Creates a new configuration object for stats gathering.
     */
    public StatsConfig(Builder builder) {
        this.publishCount = builder.publishCount;
        this.publishTotal = builder.publishTotal;
        this.publishMin = builder.publishMin;
        this.publishMax = builder.publishMax;

        this.publishMean = builder.publishMean;
        this.publishVariance = builder.publishVariance;
        this.publishStdDev = builder.publishStdDev;
        this.sampleSize = builder.sampleSize;
        this.frequencyMillis = builder.frequencyMillis;

        this.percentiles = Arrays.copyOf(builder.percentiles, builder.percentiles.length);
    }

    /**
     * Whether we should publish a 'count' statistic.
     */
    public boolean getPublishCount() {
        return publishCount;
    }

    /**
     * Whether we should publish a 'totalTime' statistic.
     */
    public boolean getPublishTotal() {
        return publishTotal;
    }

    /**
     * Whether we should publish a 'min' statistic.
     */
    public boolean getPublishMin() {
        return publishMin;
    }

    /**
     * Whether we should publish a 'max' statistic.
     */
    public boolean getPublishMax() {
        return publishMax;
    }

    /**
     * Whether we should publish an 'avg' statistic.
     */
    public boolean getPublishMean() {
        return publishMean;
    }

    /**
     * Whether we should publish a 'variance' statistic.
     */
    public boolean getPublishVariance() {
        return publishVariance;
    }

    /**
     * Whether we should publish a 'stdDev' statistic.
     */
    public boolean getPublishStdDev() {
        return publishStdDev;
    }

    /**
     * Get the size of the buffer that we should use.
     */
    public int getSampleSize() {
        return sampleSize;
    }

    /**
     * Get the frequency at which we should update all stats.
     */
    public long getFrequencyMillis() {
        return frequencyMillis;
    }

    /**
     * Get a copy of the array that holds which percentiles we should compute. The percentiles
     * are in the interval (0.0, 100.0)
     */
    public double[] getPercentiles() {
        return Arrays.copyOf(percentiles, percentiles.length);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return Objects.toStringHelper(this).
                add("publishCount", publishCount).
                add("publishTotal", publishTotal).
                add("publishMin", publishMin).
                add("publishMax", publishMax).
                add("publishMean", publishMean).
                add("publishVariance", publishVariance).
                add("publishStdDev", publishStdDev).
                add("percentiles", percentiles).
                add("sampleSize", sampleSize).
                add("frequencyMillis", frequencyMillis).
                toString();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StatsConfig)) return false;

        final StatsConfig that = (StatsConfig) o;

        if (frequencyMillis != that.frequencyMillis) return false;
        if (publishCount != that.publishCount) return false;
        if (publishMax != that.publishMax) return false;
        if (publishMean != that.publishMean) return false;
        if (publishMin != that.publishMin) return false;
        if (publishStdDev != that.publishStdDev) return false;
        if (publishTotal != that.publishTotal) return false;
        if (publishVariance != that.publishVariance) return false;
        if (sampleSize != that.sampleSize) return false;
        if (!Arrays.equals(percentiles, that.percentiles)) return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result = (publishCount ? 1 : 0);
        result = 31 * result + (publishTotal ? 1 : 0);
        result = 31 * result + (publishMin ? 1 : 0);
        result = 31 * result + (publishMax ? 1 : 0);
        result = 31 * result + (publishMean ? 1 : 0);
        result = 31 * result + (publishVariance ? 1 : 0);
        result = 31 * result + (publishStdDev ? 1 : 0);
        result = 31 * result + Arrays.hashCode(percentiles);
        result = 31 * result + sampleSize;
        result = 31 * result + (int) (frequencyMillis ^ (frequencyMillis >>> 32));
        return result;
    }
}

