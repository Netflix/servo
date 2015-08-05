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

import com.netflix.servo.util.Preconditions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration options for buckets for a {@link BucketTimer}
 * <p/>
 * For example:
 * <p/>
 * <pre>
 *      BucketConfig cfg = new BucketConfig.Builder().withBuckets(new long[]{10L, 20L}).build());
 * </pre>
 * <p/>
 * will create a configuration that will report buckets for &lt;= 10ms, (10ms, 20ms], and higher
 * than 20ms. The default timeUnit is milliseconds but can be changed using the
 * {@link BucketConfig.Builder#withTimeUnit(TimeUnit)} method.
 * <p/>
 * <p>
 * The user must specify buckets otherwise a {@link NullPointerException} will be generated.
 * </p>
 * <p/>
 * See {@link BucketTimer} for more details.
 */
public final class BucketConfig {

  /**
   * Helper class for constructing BucketConfigs.
   */
  public static class Builder {
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
    private long[] buckets = null;

    /**
     * Sets the timeUnit for the buckets.
     */
    public Builder withTimeUnit(TimeUnit timeUnit) {
      this.timeUnit = Preconditions.checkNotNull(timeUnit, "timeUnit");
      return this;
    }

    /**
     * Sets the buckets to be used.
     * <p/>
     * <p><ul>
     * <li>Each bucket must be unique.
     * <li>Buckets must be in ascending order (smallest-to-largest).
     * <li>All bucket counts will be namespaced under the "servo.bucket" tag.
     * <li>Buckets are incremented in the following way: Given a set of n
     * ordered buckets, let n1 = the first bucket. If a given duration is
     * less than or equal to n1, the counter for n1 is incremented; else
     * perform the same check on n2, n3, etc. If the duration is greater
     * the largest bucket, it is added to the 'overflow' bucket. The overflow
     * bucket is automatically created.
     * </ul>
     */
    public Builder withBuckets(long[] buckets) {
      Preconditions.checkNotNull(buckets, "buckets");

      this.buckets = Arrays.copyOf(buckets, buckets.length);
      Preconditions.checkArgument(this.buckets.length > 0, "buckets cannot be empty");
      Preconditions.checkArgument(isAscending(this.buckets),
          "buckets must be in ascending order");
      return this;
    }

    private boolean isAscending(long[] values) {
      long last = values[0];
      for (int i = 1; i < values.length; i++) {
        if (values[i] <= last) {
          return false;
        }
        last = values[i];
      }
      return true;
    }

    /**
     * Builds a new {@link com.netflix.servo.monitor.BucketConfig}.
     */
    public BucketConfig build() {
      return new BucketConfig(this);
    }
  }

  private final TimeUnit timeUnit;
  private final long[] buckets;

  private BucketConfig(Builder builder) {
    this.timeUnit = builder.timeUnit;
    this.buckets = Arrays.copyOf(builder.buckets, builder.buckets.length);
  }

  /**
   * Get the TimeUnit of the buckets.
   */
  public TimeUnit getTimeUnit() {
    return timeUnit;
  }

  /**
   * Returns an abbreviation for the Bucket's TimeUnit.
   */
  public String getTimeUnitAbbreviation() {
    switch (timeUnit) {
      case DAYS:
        return "day";
      case HOURS:
        return "hr";
      case MICROSECONDS:
        return "\u00B5s";
      case MILLISECONDS:
        return "ms";
      case MINUTES:
        return "min";
      case NANOSECONDS:
        return "ns";
      case SECONDS:
        return "s";
      default:
        return "unkwn";
    }
  }

  /**
   * Get a copy of the array that holds the bucket values.
   */
  public long[] getBuckets() {
    return Arrays.copyOf(buckets, buckets.length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "BucketConfig{timeUnit=" + timeUnit
        + ", buckets=" + Arrays.toString(buckets) + '}';
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BucketConfig)) {
      return false;
    }

    final BucketConfig that = (BucketConfig) o;
    return timeUnit == that.timeUnit && Arrays.equals(buckets, that.buckets);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = timeUnit.hashCode();
    result = 31 * result + Arrays.hashCode(buckets);
    return result;
  }
}
