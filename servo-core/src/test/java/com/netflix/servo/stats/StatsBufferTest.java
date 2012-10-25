package com.netflix.servo.stats;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class StatsBufferTest {
    double[] percentiles = { 50.0, 95.0, 99.0, 99.5 };

    private static final int SIZE = 1000;

    StatsBuffer getNoWrap() {
        StatsBuffer buffer = new StatsBuffer(SIZE, percentiles);

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
        assertEquals(buffer.getTotal(), SIZE/2 * (SIZE/2 + 1) / 2 );
    }

    @Test
    public void testVarianceNoWrap() {
        StatsBuffer buffer = getNoWrap();
        assertEquals(buffer.getVariance(), 20916.66667, 1e-4);
    }

    @Test
    public void testStdDevNoWrap() {
        StatsBuffer buffer = getNoWrap();
        assertEquals(buffer.getStdDev(), 144.62595, 1e-4);
    }

    @Test
    public void testPercentiles50NoWrap() {
        StatsBuffer buffer = getNoWrap();
        double[] percentiles = buffer.getPercentiles();
        // testNG does not give good errors if we do assertEquals on the two arrays
        assertEquals(percentiles[0], 250.5);
    }

    @Test
    public void testPercentiles95NoWrap() {
        StatsBuffer buffer = getNoWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[1], 475.95);
    }

    @Test
    public void testPercentiles99NoWrap() {
        StatsBuffer buffer = getNoWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[2], 495.99);
    }

    @Test
    public void testPercentiles995NoWrap() {
        StatsBuffer buffer = getNoWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[3], 498.495);
    }

    void assertEmpty(StatsBuffer buffer) {
        assertEquals(buffer.getCount(), 0);
        assertEquals(buffer.getTotal(), 0);
        assertEquals(buffer.getMax(), 0);
        assertEquals(buffer.getMin(), 0);

        // the following values could be NaN
        assertEquals(buffer.getMean(), 0.0);
        assertEquals(buffer.getVariance(), 0.0);
        assertEquals(buffer.getStdDev(), 0.0);
        assertEquals(buffer.getPercentiles()[0], 0.0);
    }

    @Test
    public void testEmptyBuffer() {
        StatsBuffer buffer = new StatsBuffer(SIZE, percentiles);
        assertEmpty(buffer);

        buffer.computeStats();
        assertEmpty(buffer);
    }

    StatsBuffer getWithWrap() {
        StatsBuffer buffer = new StatsBuffer(SIZE, percentiles);
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
        assertEquals(buffer.getCount(), SIZE * 2);
    }

    final static long expectedTotalWrap = SIZE*2 * (SIZE*2 + 1) / 2;
    @Test
    public void testTotalWrap() {
        StatsBuffer buffer = getWithWrap();
        assertEquals(buffer.getTotal(), expectedTotalWrap );
    }

    @Test
    public void testMeanWrap() {
        StatsBuffer buffer = getWithWrap();
        assertEquals(buffer.getMean(), (double)expectedTotalWrap / (SIZE * 2));
    }

    final static double expectedVarianceWrap = 1667666.75;
    @Test
    public void testVarianceWrap() {
        StatsBuffer buffer = getWithWrap();
        assertEquals(buffer.getVariance(), expectedVarianceWrap, 1e-4);
    }

    @Test
    public void testStdDevWrap() {
        StatsBuffer buffer = getWithWrap();
        assertEquals(buffer.getStdDev(), Math.sqrt(expectedVarianceWrap), 1e-4);
    }

    @Test
    public void testPercentiles50Wrap() {
        StatsBuffer buffer = getWithWrap();
        double[] percentiles = buffer.getPercentiles();
        // testNG does not give good errors if we do assertEquals on the two arrays
        assertEquals(percentiles[0], 501.0);
    }

    @Test
    public void testPercentiles95Wrap() {
        StatsBuffer buffer = getWithWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[1], 951.0);
    }

    @Test
    public void testPercentiles99Wrap() {
        StatsBuffer buffer = getWithWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[2], 991.0);
    }

    @Test
    public void testPercentiles995Wrap() {
        StatsBuffer buffer = getWithWrap();
        double[] percentiles = buffer.getPercentiles();
        assertEquals(percentiles[3], 996.0);
    }


}
