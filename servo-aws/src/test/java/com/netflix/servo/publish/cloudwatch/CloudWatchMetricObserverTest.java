/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish.cloudwatch;

import com.amazonaws.AmazonClientException;

import com.amazonaws.auth.BasicAWSCredentials;

import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.Metric;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

/**
 * User: gorzell
 * Date: 1/9/12
 * Time: 2:24 PM
 */
public class CloudWatchMetricObserverTest {
    private CloudWatchMetricObserver observer = new CloudWatchMetricObserver("testObserver", "testDomain", new BasicAWSCredentials("", ""));

    @Test
    public void testUpdate() throws Exception {
        List<Metric> metrics = new ArrayList<Metric>(33);
        for (int i = 0; i < 33; i++) {
            metrics.add(new Metric("test", BasicTagList.EMPTY, System.currentTimeMillis(), 10));
        }

        try{
            observer.update(metrics);
        } catch (AmazonClientException e){
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateDimensions() throws Exception {

    }

    @Test
    public void testCreateMetricDatum() throws Exception {

    }

    @Test
    public void testCreatePutRequest() throws Exception {

    }

    @Test
    public void testTruncate() throws Exception {
        observer.withTruncateEnabled(true);
        Assert.assertEquals(CloudWatchMetricObserver.LARGEST_SENDABLE, observer.truncate(Double.MAX_VALUE));
        Assert.assertEquals(- CloudWatchMetricObserver.LARGEST_SENDABLE, observer.truncate(- Double.MAX_VALUE));
        Assert.assertEquals(0.0, observer.truncate(Double.MIN_VALUE));
        Assert.assertEquals(0.0, observer.truncate(- Double.MIN_VALUE));

        Assert.assertEquals(1.0, observer.truncate(1.0));
        Assert.assertEquals(10000.0, observer.truncate(10000.0));
        Assert.assertEquals(0.0, observer.truncate(0.0));
        observer.withTruncateEnabled(false);
    }
}
