/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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

import com.amazonaws.auth.AWSCredentials;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

import com.google.common.base.Preconditions;

import com.netflix.servo.Tag;
import com.netflix.servo.TagList;

import com.netflix.servo.aws.DataSourceTypeToAwsUnit;

import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.Metric;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Writes observations to Amazon's CloudWatch.
 */
public class CloudWatchMetricObserver extends BaseMetricObserver {

    private static final Logger log =
            LoggerFactory.getLogger(CloudWatchMetricObserver.class);

    private int batchSize;

    private final AmazonCloudWatch cloudWatch;
    private final String cloudWatchNamespace;

    /**
     *
     * @param name Unique name of the observer.
     * @param cloudWatchNamespace Namespace to use in CloudWatch.
     * @param credentials Amazon credentials.
     */
    public CloudWatchMetricObserver(String name, String cloudWatchNamespace, AWSCredentials credentials) {
        super(name);
        cloudWatch = new AmazonCloudWatchClient(credentials);
        this.cloudWatchNamespace = cloudWatchNamespace;
        batchSize = 20;
    }

    /**
     *
     * @param name Unique name of the observer.
     * @param cloudWatchNamespace Namespace to use in CloudWatch.
     * @param credentials Amazon credentials.
     * @param batchSize Batch size to send to Amazon.  They currently enforce a max of 20.
     */
    public CloudWatchMetricObserver(String name, String cloudWatchNamespace, AWSCredentials credentials, int batchSize) {
        this(name, cloudWatchNamespace, credentials);
        this.batchSize = batchSize;
    }

    /**
     *
     * @param metrics The list of metrics you want to send to CloudWatch
     */
    public void updateImpl(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);

        List<Metric> batch = new ArrayList<Metric>(batchSize);
        int batchCount = 1;

        while (metrics.size() > 0) {
            batch.add(metrics.remove(0));

            if (batchCount++ % batchSize == 0) {
                cloudWatch.putMetricData(createPutRequest(batch));
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            cloudWatch.putMetricData(createPutRequest(batch));
        }
    }

    PutMetricDataRequest createPutRequest(List<Metric> batch) {
        List<MetricDatum> datumList = new ArrayList<MetricDatum>(batch.size());

        for (Metric m : batch) {
            datumList.add(createMetricDatum(m));
        }

        return new PutMetricDataRequest().withNamespace(cloudWatchNamespace)
                .withMetricData(datumList);
    }

    MetricDatum createMetricDatum(Metric metric) {
        MetricDatum metricDatum = new MetricDatum();

        return metricDatum.withMetricName(metric.getConfig().getName())
                .withDimensions(createDimensions(metric.getConfig().getTags()))
                .withUnit("None")//DataSourceTypeToAwsUnit.getUnit(metric.))
                .withTimestamp(new Date(metric.getTimestamp()))
                .withValue(Double.valueOf(metric.getValue().doubleValue()));
        //TODO Need to convert into reasonable units based on DataType
    }

    List<Dimension> createDimensions(TagList tags) {
        List<Dimension> dimensionList = new ArrayList<Dimension>(tags.size());

        for (Tag tag : tags) {
            dimensionList.add(new Dimension().withName(tag.getKey()).withValue(tag.getValue()));
        }

        return dimensionList;
    }


}
