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
package com.netflix.servo.publish.cloudwatch;

import com.amazonaws.AmazonClientException;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;

import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.Metric;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * CloudWatchMetricObserver tests.
 */
public class CloudWatchMetricObserverTest {
    private CloudWatchMetricObserver observer = new CloudWatchMetricObserver(
            "testObserver", "testDomain", new InstanceProfileCredentialsProvider());

    private static final int NUM_METRICS = 33;
    private static final int VALUE = 10;

    /**
     * Update.
     */
    @Test
    public void testUpdate() throws Exception {
        List<Metric> metrics = new ArrayList<Metric>(NUM_METRICS);
        for (int i = 0; i < NUM_METRICS; i++) {
            metrics.add(new Metric("test", BasicTagList.EMPTY, System.currentTimeMillis(), VALUE));
        }

        try {
            observer.update(metrics);
        } catch (AmazonClientException e) {
            e.printStackTrace();
        }
    }

    /**
     * create dimensions.
     */
    @Test
    public void testCreateDimensions() throws Exception {

    }

    /**
     * create metric datum.
     */
    @Test
    public void testCreateMetricDatum() throws Exception {

    }

    /**
     * create put request.
     */
    @Test
    public void testCreatePutRequest() throws Exception {

    }

    @Test
    public void testTruncate() throws Exception {
        observer.withTruncateEnabled(true);
        Assert.assertEquals(CloudWatchMetricObserver.MAX_VALUE, observer.truncate(Double.POSITIVE_INFINITY));
        Assert.assertEquals(-CloudWatchMetricObserver.MAX_VALUE, observer.truncate(Double.NEGATIVE_INFINITY));
        Assert.assertEquals(CloudWatchMetricObserver.MAX_VALUE, observer.truncate(Double.MAX_VALUE));
        Assert.assertEquals(-CloudWatchMetricObserver.MAX_VALUE, observer.truncate(-Double.MAX_VALUE));
        Assert.assertEquals(0.0, observer.truncate(Double.MIN_VALUE));
        Assert.assertEquals(0.0, observer.truncate(-Double.MIN_VALUE));

        Assert.assertEquals(1.0, observer.truncate(1.0));
        Assert.assertEquals(10000.0, observer.truncate(10000.0));
        Assert.assertEquals(0.0, observer.truncate(0.0));

        Assert.assertEquals(0.0, observer.truncate(Double.NaN));
        observer.withTruncateEnabled(false);
    }
}
