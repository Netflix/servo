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
package com.netflix.servo.stats;

import java.util.Arrays;

/**
 * Configuration options for a {@link com.netflix.servo.monitor.StatsTimer}
 * <p/>
 * By default we publish count (number of times the timer was executed), totalTime, and
 * 95.0, and 99.0 percentiles.
 * <p/>
 * The size for the buffer used to store samples is controlled using the sampleSize field,
 * and the frequency
 * at which stats are computed is controlled with the computeFrequencyMillis option.
 * By default these are
 * set to 100,000 entries in the buffer, and computation at 60,000 ms (1 minute) intervals.
 */
public final class StatsConfig {
  private static final String CLASS_NAME = StatsConfig.class.getCanonicalName();
  private static final String SIZE_PROP = CLASS_NAME + ".sampleSize";
  private static final String FREQ_PROP = CLASS_NAME + ".computeFreqMillis";

  /**
   * Builder for StatsConfig. By default the configuration includes count,
   * total and 95th and 99th percentiles.
   */
  public static class Builder {
    private boolean publishCount = true;
    private boolean publishTotal = true;
    private boolean publishMin = false;
    private boolean publishMax = false;
    private boolean publishMean = false;
    private boolean publishVariance = false;
    private boolean publishStdDev = false;
    private int sampleSize = Integer.parseInt(System.getProperty(SIZE_PROP, "1000"));
    private long frequencyMillis = Long.parseLong(System.getProperty(FREQ_PROP, "60000"));

    private double[] percentiles = {95.0, 99.0};

    /**
     * Whether to publish count or not.
     */
    public Builder withPublishCount(boolean publishCount) {
      this.publishCount = publishCount;
      return this;
    }

    /**
     * Whether to publish total or not.
     */
    public Builder withPublishTotal(boolean publishTotal) {
      this.publishTotal = publishTotal;
      return this;
    }

    /**
     * Whether to publish min or not.
     */
    public Builder withPublishMin(boolean publishMin) {
      this.publishMin = publishMin;
      return this;
    }

    /**
     * Whether to publish max or not.
     */
    public Builder withPublishMax(boolean publishMax) {
      this.publishMax = publishMax;
      return this;
    }

    /**
     * Whether to publish an average statistic or not. Note that if you plan
     * to aggregate the values reported (for example across a cluster of nodes) you probably do
     * not want to publish the average per node, and instead want to compute it by publishing
     * total and count.
     */
    public Builder withPublishMean(boolean publishMean) {
      this.publishMean = publishMean;
      return this;
    }

    /**
     * Whether to publish variance or not.
     */
    public Builder withPublishVariance(boolean publishVariance) {
      this.publishVariance = publishVariance;
      return this;
    }


    /**
     * Whether to publish standard deviation or not.
     */
    public Builder withPublishStdDev(boolean publishStdDev) {
      this.publishStdDev = publishStdDev;
      return this;
    }

    /**
     * Set the percentiles to compute.
     *
     * @param percentiles An array of doubles describing which percentiles to compute. For
     *                    example {@code {95.0, 99.0}}
     */
    public Builder withPercentiles(double[] percentiles) {
      this.percentiles = Arrays.copyOf(percentiles, percentiles.length);
      return this;
    }

    /**
     * Set the sample size.
     */
    public Builder withSampleSize(int size) {
      this.sampleSize = size;
      return this;
    }

    /**
     * How often to compute the statistics. Usually this will be set to the main
     * poller interval. (Default is 60s.)
     */
    public Builder withComputeFrequencyMillis(long frequencyMillis) {
      this.frequencyMillis = frequencyMillis;
      return this;
    }

    /**
     * Create a new StatsConfig object.
     */
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

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "StatsConfig{"
        + "publishCount=" + publishCount
        + ", publishTotal=" + publishTotal
        + ", publishMin=" + publishMin
        + ", publishMax=" + publishMax
        + ", publishMean=" + publishMean
        + ", publishVariance=" + publishVariance
        + ", publishStdDev=" + publishStdDev
        + ", percentiles=" + Arrays.toString(percentiles)
        + ", sampleSize=" + sampleSize
        + ", frequencyMillis=" + frequencyMillis
        + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof StatsConfig)) {
      return false;
    }

    final StatsConfig that = (StatsConfig) o;
    return frequencyMillis == that.frequencyMillis
        && publishCount == that.publishCount
        && publishMax == that.publishMax
        && publishMean == that.publishMean
        && publishMin == that.publishMin
        && publishStdDev == that.publishStdDev
        && publishTotal == that.publishTotal
        && publishVariance == that.publishVariance
        && sampleSize == that.sampleSize
        && Arrays.equals(percentiles, that.percentiles);

  }

  /**
   * {@inheritDoc}
   */
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

