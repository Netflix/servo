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

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.assertEquals;

public class StatsBufferTest {
  static final double[] PERCENTILES = {50.0, 95.0, 99.0, 99.5};

  private static final int SIZE = 1000;

  StatsBuffer getNoWrap() {
    StatsBuffer buffer = new StatsBuffer(SIZE, PERCENTILES);

    int max = SIZE / 2;
    for (int i = 0; i <= max; ++i) {
      buffer.record(i);
    }

    buffer.computeStats();

    return buffer;
  }

  @Test
  public void testMaxNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getMax(), SIZE / 2);
  }

  @Test
  public void testMinNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getMin(), 0);
  }

  @Test
  public void testMeanNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getMean(), SIZE / 4.0);
  }

  @Test
  public void testCountNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getCount(), SIZE / 2 + 1);
  }

  @Test
  public void testTotalNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getTotalTime(), SIZE / 2 * (SIZE / 2 + 1) / 2);
  }

  @Test
  public void testVarianceNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getVariance(), 20958.5, 1e-4);
  }

  @Test
  public void testStdDevNoWrap() {
    StatsBuffer buffer = getNoWrap();
    assertEquals(buffer.getStdDev(), 144.77051, 1e-4);
  }

  @Test
  public void testPercentiles50NoWrap() {
    StatsBuffer buffer = getNoWrap();
    double[] percentiles = buffer.getPercentileValues();
    // testNG does not give good errors if we do assertEquals on the two arrays
    assertEquals(percentiles[0], 250.5);
  }

  @Test
  public void testPercentiles95NoWrap() {
    StatsBuffer buffer = getNoWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[1], 475.95);
  }

  @Test
  public void testPercentiles99NoWrap() {
    StatsBuffer buffer = getNoWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[2], 495.99);
  }

  @Test
  public void testPercentiles995NoWrap() {
    StatsBuffer buffer = getNoWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[3], 498.495);
  }

  void assertEmpty(StatsBuffer buffer) {
    assertEquals(buffer.getCount(), 0);
    assertEquals(buffer.getTotalTime(), 0);
    assertEquals(buffer.getMax(), 0);
    assertEquals(buffer.getMin(), 0);

    // the following values could be NaN
    assertEquals(buffer.getMean(), 0.0);
    assertEquals(buffer.getVariance(), 0.0);
    assertEquals(buffer.getStdDev(), 0.0);
    assertEquals(buffer.getPercentileValues()[0], 0.0);
  }

  @Test
  public void testEmptyBuffer() {
    StatsBuffer buffer = new StatsBuffer(SIZE, PERCENTILES);
    assertEmpty(buffer);

    buffer.computeStats();
    assertEmpty(buffer);
  }

  StatsBuffer getWithWrap() {
    StatsBuffer buffer = new StatsBuffer(SIZE, PERCENTILES);
    for (int i = SIZE * 2; i > 0; --i) {
      buffer.record(i);
    }
    buffer.computeStats();
    return buffer;
  }

  @Test
  public void testMaxWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getMax(), SIZE);
  }

  @Test
  public void testMinWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getMin(), 1);
  }

  @Test
  public void testCountWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getCount(), SIZE);
  }

  static final long EXPECTED_TOTAL_WRAP = SIZE * (SIZE + 1) / 2;

  @Test
  public void testTotalWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getTotalTime(), EXPECTED_TOTAL_WRAP);
  }

  @Test
  public void testMeanWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getMean(), (double) EXPECTED_TOTAL_WRAP / SIZE);
  }

  static final double EXPECTED_VARIANCE_WRAP = 83416.66667;

  @Test
  public void testVarianceWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getVariance(), EXPECTED_VARIANCE_WRAP, 1e-4);
  }

  @Test
  public void testStdDevWrap() {
    StatsBuffer buffer = getWithWrap();
    assertEquals(buffer.getStdDev(), Math.sqrt(EXPECTED_VARIANCE_WRAP), 1e-4);
  }

  @Test
  public void testPercentiles50Wrap() {
    StatsBuffer buffer = getWithWrap();
    double[] percentiles = buffer.getPercentileValues();
    // testNG does not give good errors if we do assertEquals on the two arrays
    assertEquals(percentiles[0], 501.0);
  }

  @Test
  public void testPercentiles95Wrap() {
    StatsBuffer buffer = getWithWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[1], 951.0);
  }

  @Test
  public void testPercentiles99Wrap() {
    StatsBuffer buffer = getWithWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[2], 991.0);
  }

  @Test
  public void testPercentiles995Wrap() {
    StatsBuffer buffer = getWithWrap();
    double[] percentiles = buffer.getPercentileValues();
    assertEquals(percentiles[3], 996.0);
  }

  // Used to access private count field via reflection so we can quickly simulate
  // a count that will cause an integer overflow.
  private void setCount(StatsBuffer buffer, int v) throws Exception {
    Class<?> cls = buffer.getClass();
    Field field = cls.getDeclaredField("pos");
    field.setAccessible(true);
    field.set(buffer, v);
  }

  // Before fix this would throw an ArrayIndexOutOfBoundException
  @Test
  public void testCountOverflow() throws Exception {
    StatsBuffer buffer = new StatsBuffer(SIZE, PERCENTILES);
    setCount(buffer, Integer.MAX_VALUE);
    buffer.record(1);
    buffer.record(2);
  }

  // java.lang.IllegalArgumentException: fromIndex(0) > toIndex(-2147483647)
  @Test
  public void testComputeStatsWithOverflow() throws Exception {
    StatsBuffer buffer = new StatsBuffer(SIZE, PERCENTILES);
    setCount(buffer, Integer.MAX_VALUE);
    buffer.record(1);
    buffer.record(2);
    buffer.computeStats();
  }
}
