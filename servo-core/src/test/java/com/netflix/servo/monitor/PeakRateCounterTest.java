/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

import static com.netflix.servo.monitor.PeakRateCounterTest.SAMPL_INTERVAL;
import com.netflix.servo.tag.Tag;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import org.testng.annotations.Test;

public class PeakRateCounterTest extends AbstractMonitorTest<PeakRateCounter> {

    /* testing Note:
     * the value of SAMPL_INTERVAL will cause the threading test to run for atleast that long
     * so if it starts causing unit tests to run too long, try shortening it.
     * long running threading tests probably belong somewhere else since they
     * slow down test driven approach, not in these
     * unit tests (where to put them?)
     */
    static final long SAMPL_INTERVAL = 30;
    static final TimeUnit UNIT = TimeUnit.SECONDS;
    static final long SAMPL_INTERVAL_MILLIS = TimeUnit.MILLISECONDS.convert(SAMPL_INTERVAL, UNIT);

    @Override
    public PeakRateCounter newInstance(String name) {
        return new PeakRateCounter(MonitorConfig.builder(name).build());

    }

    @Test
    public void testIncrementBucket() throws Exception {
        PeakRateCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L, "no buckets, no delta, no max");

        //first bucket, continual increase and peak
        long bucketOneKey = 1;
        long bucketTwoKey = 2;
        long bucketThreeKey = 3;
        long bucketKey;
        AtomicLong bucketValue;
        Map.Entry<Long, AtomicLong> maxBucket;

        c.incrementBucket(bucketOneKey, 1L);
        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 1L);
        peakCount = c.getValue();
        assertEquals(peakCount, 1L, "Delta 1, Peak 1");

        c.incrementBucket(bucketOneKey, 1L);
        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 2L);
        peakCount = c.getValue();
        assertEquals(peakCount, 2L, "Delta 1, Peak 2");

        c.incrementBucket(bucketOneKey, 1L);
        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 3L);
        peakCount = c.getValue();
        assertEquals(peakCount, 3L, "Delta 1, Peak 3");

        c.incrementBucket(bucketOneKey, 1L);
        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 4L);
        peakCount = c.getValue();
        assertEquals(peakCount, 4L, "Delta 1, Peak 4");

        c.incrementBucket(bucketOneKey, 1L);
        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 5L);
        peakCount = c.getValue();
        assertEquals(peakCount, 5L, "Delta 1, Peak 5");

        //2nd bucket becomes the max

        c.incrementBucket(bucketTwoKey, 10L);

        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 5L, "bucketOne unchanged");
        bucketValue = c.getBucketValue(bucketTwoKey);
        assertEquals(bucketValue.get(), 10L, "bucketTwo new max");
        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta 10, Peak 10, bucketTwo");

        //1st bucket incremented but still not the max
        c.incrementBucket(bucketOneKey, 1L);

        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 6L, "bucketOne incremented by 1");
        bucketValue = c.getBucketValue(bucketTwoKey);
        assertEquals(bucketValue.get(), 10L, "bucketTwo unchanged, still max");
        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta 1, Bucket 1 incremented to 6 and Peak still 10 from Bucket two");

        //1st bucket equals the max
        c.incrementBucket(bucketOneKey, 4L);

        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 10L, "bucketOne now has max count");
        bucketValue = c.getBucketValue(bucketTwoKey);
        assertEquals(bucketValue.get(), 10L, "bucketTwo unchanged, still max");
        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta 4, Bucket 1 incremented to 10, Peak still 10");
        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, bucketTwoKey, "bucket two is still the max even though bucket 1 count now equals max");


        //1st bucket now greater than the old max
        c.incrementBucket(bucketOneKey, 1L);

        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 11L, "bucketOne now has new max count");
        bucketValue = c.getBucketValue(bucketTwoKey);
        assertEquals(bucketValue.get(), 10L, "bucketTwo unchanged, still old max");
        peakCount = c.getValue();
        assertEquals(peakCount, 11L, "Delta 1, Bucket 1 incremented to 11, Peak now 11");
        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, bucketOneKey, "bucket one is new max");


        //increment bucket three to become new peak

        c.incrementBucket(bucketThreeKey, 20L);

        bucketValue = c.getBucketValue(bucketOneKey);
        assertEquals(bucketValue.get(), 11L, "bucketOne unchanged, now is old max");
        bucketValue = c.getBucketValue(bucketTwoKey);
        assertEquals(bucketValue.get(), 10L, "bucketTwo unchanged");
        bucketValue = c.getBucketValue(bucketThreeKey);
        assertEquals(bucketValue.get(), 20L, "bucketThree is new max");

        peakCount = c.getValue();
        assertEquals(peakCount, 20L, "Delta 20, Bucket 3 new peak");
        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, bucketThreeKey, "bucket three is new max");

    }

    @Test
    public void testTrimBuckets() throws Exception {
        PeakRateCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L, "no buckets, no delta, no max");

        //first bucket, continual increase and peak
        long maxBucketKey = 10;
        long currentBucketKey = 20;
        long oldBucketAKey = 1;
        long oldBucketBKey = 2;

        long bucketKey;
        AtomicLong bucketValue;
        Map.Entry<Long, AtomicLong> maxBucket;

        c.incrementBucket(oldBucketAKey, 5L);
        c.incrementBucket(oldBucketBKey, 30L);
        c.incrementBucket(currentBucketKey, 100L);
        c.incrementBucket(maxBucketKey, 200L);

        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, maxBucketKey);

        c.trimBuckets(currentBucketKey);

        bucketValue = c.getBucketValue(oldBucketAKey);
        assertEquals(bucketValue, null, "oldBucketA trimmed");
        bucketValue = c.getBucketValue(oldBucketBKey);
        assertEquals(bucketValue, null, "oldBucketB trimmed");

        bucketValue = c.getBucketValue(maxBucketKey);
        assertEquals(bucketValue.get(), 200L, "max bucket not trimmed");
        bucketValue = c.getBucketValue(currentBucketKey);
        assertEquals(bucketValue.get(), 100L, "current bucket not trimmed");

        c.incrementBucket(currentBucketKey, 100L);
        
        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, currentBucketKey);

        c.trimBuckets(currentBucketKey);
        
        
        bucketValue = c.getBucketValue(maxBucketKey);
        assertEquals(bucketValue, null, "old max bucket  trimmed");
        bucketValue = c.getBucketValue(currentBucketKey);
        assertEquals(bucketValue.get(), 200L, "current bucket is also the max, not trimmed");

        c.incrementBucket(currentBucketKey, 100L);
        maxBucket = c.getMaxBucket();
        bucketKey = maxBucket.getKey();
        assertEquals(bucketKey, currentBucketKey);

        c.trimBuckets(currentBucketKey);
        
        bucketValue = c.getBucketValue(currentBucketKey);
        assertEquals(bucketValue.get(), 300L, "current bucket equals new max, not trimmed");

    }

    @Test
    public void testIncrement() throws Exception {
        PeakRateCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L);


        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            c.increment();
        }

        peakCount = c.getValue();
        assertEquals(peakCount, 1L, "Delta of 5 in 5 seconds, e.g. peak rate = average, 1 per second");



        for (int i = 0; i < 5; i++) {
            Thread.sleep(1000L);
            c.increment(3);
        }

        peakCount = c.getValue();
        assertEquals(peakCount, 3L, "Delta of 15 in 5 seconds, e.g. peak rate = average, 3 per second");


        Thread.sleep(1000L);
        c.increment(10);
        for (int i = 0; i < 3; i++) {
            Thread.sleep(1000L);
            c.increment(3);
        }
        c.increment();

        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta of 15 in 5 seconds, e.g. peak rate = 10, average = 3, min = 1 per second");


        Thread.sleep(5000L);
        peakCount = c.getValue();
        assertEquals(peakCount, 10L, "Delta of 0 in 5 seconds, e.g. peak rate = previous max, 10 per second");


    }

    @Test
    public void testReset() throws Exception {
        PeakRateCounter c = newInstance("foo");
        long peakCount = c.getValue();
        assertEquals(peakCount, 0L);

        c.increment();
        peakCount = c.getValue();
        assertEquals(peakCount, 1L, "Delta 1 in first second");

        Thread.sleep(1000L);
        c.increment(5L);
        peakCount = c.getValue();
        assertEquals(peakCount, 5L, "Delta 5 in second second");

        Thread.sleep(2000L);
        c.increment(10L);
        peakCount = c.getAndResetValue();
        assertEquals(peakCount, 10L, "Delta 10 in fourth second before reset");

        peakCount = c.getValue();
        assertEquals(peakCount, 0, "After Reset");

        c.increment(8L);
        peakCount = c.getValue();
        assertEquals(peakCount, 8L, "Delta 8 in first second after reset");

    }

    @Test
    public void testHasGaugeTag() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag("type");
        assertEquals(type.getValue(), "GAUGE");
    }

    @Test
    public void testEqualsCount() throws Exception {
        PeakRateCounter c1 = newInstance("foo");
        PeakRateCounter c2 = newInstance("foo");
        assertEquals(c1, c2);

        c1.increment();
        assertNotEquals(c1, c2);
        c2.increment();
        assertEquals(c1, c2);
    }

    @Test
    public void testEqualsAndHashCodeName() throws Exception {
        PeakRateCounter c1 = newInstance("1234567890");
        PeakRateCounter c2 = newInstance("1234567890");
        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
        c2 = c1;
        assertEquals(c2, c1);
    }
}
