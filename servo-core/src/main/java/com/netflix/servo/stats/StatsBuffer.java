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

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A simple circular buffer that records values, and computes useful stats. This implementation is not thread
 * safe.
 */
public class StatsBuffer {
    private int count;
    private double mean;
    private double sumSquares;
    private double variance;
    private double stddev;
    private long min;
    private long max;
    private long total;

    private double[] percentiles;
    private double[] percentileValues;
    private final int size;
    private final long[] values;
    private AtomicBoolean statsComputed = new AtomicBoolean(false);

    /**
     * Create a circular buffer that will be used to record values and compute useful stats
     * @param size      The capacity of the buffer
     * @param percentiles  Array of percentiles to compute. For example { 95.0, 99.0 }. If no percentileValues are required
     *                  pass a 0-sized array.
     */
    public StatsBuffer(int size, double[] percentiles) {
        Preconditions.checkArgument(size > 0, "Size of the buffer must be greater than 0");
        Preconditions.checkArgument(percentiles != null,
                "Percents array must be non-null. Pass a 0-sized array if you don't want any percentileValues to be computed.");
        Preconditions.checkArgument(validPercentiles(percentiles), "All percentiles should be in the interval (0.0, 100.0]");
        values = new long[size];
        this.size = size;
        this.percentiles = Arrays.copyOf(percentiles, percentiles.length);
        this.percentileValues = new double[percentiles.length];

        reset();
    }

    private static boolean validPercentiles(double[] percentiles) {
        for (double percentile : percentiles) {
            if (percentile <= 0.0 || percentile > 100.0) return false;
        }
        return true;
    }

    /**
     * Reset our local state: All values are set to 0.
     */
    public void reset() {
        statsComputed.set(false);
        count = 0;
        total = 0L;
        mean = 0.0;
        variance = 0.0;
        stddev = 0.0;
        min = 0L;
        max = 0L;
        sumSquares = 0.0;
        for (int i = 0; i < percentileValues.length; ++i) {
            percentileValues[i] = 0.0;
        }
    }

    /**
     * Record a new value for this buffer.
     */
    public void record(long n) {
        values[count++ % size] = n;
        total += n;
        sumSquares += n * n;
    }

    /**
     * Compute stats for the current set of values.
     */
    public void computeStats() {
        if (statsComputed.getAndSet(true)) return;

        if (count == 0) return;

        int curSize = Math.min(count, size);
        Arrays.sort(values, 0, curSize); // to compute percentileValues
        min = values[0];
        max = values[curSize - 1];
        mean = (double)total / count;
        variance = (sumSquares / curSize) - (mean * mean);
        stddev = Math.sqrt(variance);
        computePercentiles(curSize);
    }

    private void computePercentiles(int curSize) {
        for (int i = 0; i < percentiles.length; ++i) {
            percentileValues[i] = calcPercentile(curSize, percentiles[i]);
        }
    }

    private double calcPercentile(int curSize, double percent) {
        if (curSize == 0) {
            return 0.0;
        }
        if (curSize == 1) {
            return values[0];
        }

        /*
         * We use the definition from http://cnx.org/content/m10805/latest
         * modified for 0-indexed arrays.
         */
        final double rank = percent * curSize / 100.0; // SUPPRESS CHECKSTYLE MagicNumber
        final int ir = (int) Math.floor(rank);
        final int irNext = ir + 1;
        final double fr = rank - ir;
        if (irNext >= curSize) {
            return values[curSize - 1];
        } else if (fr == 0.0) {
            return values[ir];
        } else {
            // Interpolate between the two bounding values
            final double lower = values[ir];
            final double upper = values[irNext];
            return fr * (upper - lower) + lower;
        }
    }

    /**
     * Get the number of entries recorded.
     */
    public int getCount() {
        return count;
    }

    /**
     * Get the average of the values recorded.
     *
     * @return The average of the values recorded, or 0.0 if no values were recorded.
     */
    public double getMean() {
        return mean;
    }

    /**
     * Get the variance for the population of the recorded values present in our buffer.
     *
     * @return The variance.p of the values recorded, or 0.0 if no values were recorded.
     */
    public double getVariance() {
        return variance;
    }

    /**
     * Get the standard deviation for the population of the recorded values present in our buffer.
     *
     * @return The stddev.p of the values recorded, or 0.0 if no values were recorded.
     */
    public double getStdDev() {
        return stddev;
    }

    /**
     * Get the minimum of the values currently in our buffer.
     *
     * @return The min of the values recorded, or 0.0 if no values were recorded.
     */
    public long getMin() {
        return min;
    }

    /**
     * Get the max of the values currently in our buffer.
     *
     * @return The max of the values recorded, or 0.0 if no values were recorded.
     */
    public long getMax() {
        return max;
    }

    /**
     * Get the total sum of the values recorded.
     * @return The sum of the values recorded, or 0.0 if no values were recorded.
     */
    public long getTotalTime() {
        return total;
    }

    /**
     * Get the computed percentileValues. See {@link StatsConfig} for how to request different
     * percentileValues. Note that for efficiency reasons we return the actual array of computed values.
     * Users must NOT modify this array.
     *
     * @return An array of computed percentileValues.
     */
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "EI_EXPOSE_REP",
            justification = "Performance critical code. Users treat it as read-only")
    public double[] getPercentileValues() {
        return percentileValues;
    }

    /**
     * Return the percentiles we will compute: For example: 95.0, 99.0.
     */
    public double[] getPercentiles() {
        return Arrays.copyOf(percentiles, percentiles.length);
    }
}
