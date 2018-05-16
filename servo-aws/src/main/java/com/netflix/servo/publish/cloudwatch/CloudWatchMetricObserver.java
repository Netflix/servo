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
package com.netflix.servo.publish.cloudwatch;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.netflix.servo.DefaultMonitorRegistry;
import com.netflix.servo.Metric;
import com.netflix.servo.aws.AwsServiceClients;
import com.netflix.servo.monitor.BasicTimer;
import com.netflix.servo.monitor.Counter;
import com.netflix.servo.monitor.DynamicCounter;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.monitor.StepCounter;
import com.netflix.servo.monitor.Stopwatch;
import com.netflix.servo.monitor.Timer;
import com.netflix.servo.publish.BaseMetricObserver;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import com.netflix.servo.util.Preconditions;
import com.netflix.servo.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Writes observations to Amazon's CloudWatch.
 */
public class CloudWatchMetricObserver extends BaseMetricObserver {

  private static final Logger LOG = LoggerFactory.getLogger(CloudWatchMetricObserver.class);

  /**
   * Experimentally derived value for the largest exponent that can be sent to cloudwatch
   * without triggering an InvalidParameterValue exception. See CloudWatchValueTest for the test
   * program that was used.
   */
  private static final int MAX_EXPONENT = 360;

  /**
   * Experimentally derived value for the smallest exponent that can be sent to cloudwatch
   * without triggering an InvalidParameterValue exception. See CloudWatchValueTest for the test
   * program that was used.
   */
  private static final int MIN_EXPONENT = -360;

  /**
   * Maximum value that can be represented in cloudwatch.
   */
  static final double MAX_VALUE = java.lang.Math.pow(2.0, MAX_EXPONENT);

  /**
   * Number of cloudwatch metrics reported.
   */
  private static final Counter METRICS_COUNTER = new StepCounter(
      new MonitorConfig.Builder("servo.cloudwatch.metrics").build());

  /**
   * Number of cloudwatch put calls.
   */
  private static final Timer PUTS_TIMER = new BasicTimer(
      new MonitorConfig.Builder("servo.cloudwatch.puts").build());

  /**
   * Number of cloudwatch errors.
   */
  private static final MonitorConfig ERRORS_COUNTER_ID =
      new MonitorConfig.Builder("servo.cloudwatch.errors").build();

  static {
    DefaultMonitorRegistry.getInstance().register(METRICS_COUNTER);
    DefaultMonitorRegistry.getInstance().register(PUTS_TIMER);
  }

  private int batchSize;
  private boolean truncateEnabled = false;

  private final AmazonCloudWatch cloudWatch;
  private final String cloudWatchNamespace;

  /**
   * @param name        Unique name of the observer.
   * @param namespace   Namespace to use in CloudWatch.
   * @param credentials Amazon credentials.
   * @deprecated use equivalent constructor that accepts an AWSCredentialsProvider.
   */
  @Deprecated
  public CloudWatchMetricObserver(String name, String namespace, AWSCredentials credentials) {
    this(name, namespace, new AmazonCloudWatchClient(credentials));
  }

  /**
   * @param name        Unique name of the observer.
   * @param namespace   Namespace to use in CloudWatch.
   * @param credentials Amazon credentials.
   * @param batchSize   Batch size to send to Amazon.  They currently enforce a max of 20.
   * @deprecated use equivalent constructor that accepts an AWSCredentialsProvider.
   */
  @Deprecated
  public CloudWatchMetricObserver(String name, String namespace, AWSCredentials credentials,
                                  int batchSize) {
    this(name, namespace, credentials);
    this.batchSize = batchSize;
  }

  /**
   * @param name      Unique name of the observer.
   * @param namespace Namespace to use in CloudWatch.
   * @param provider  Amazon credentials provider
   */
  public CloudWatchMetricObserver(String name, String namespace,
                                  AWSCredentialsProvider provider) {
    this(name, namespace, AwsServiceClients.cloudWatch(provider));
  }

  /**
   * @param name      Unique name of the observer.
   * @param namespace Namespace to use in CloudWatch.
   * @param provider  Amazon credentials provider.
   * @param batchSize Batch size to send to Amazon.  They currently enforce a max of 20.
   */
  public CloudWatchMetricObserver(String name, String namespace,
                                  AWSCredentialsProvider provider, int batchSize) {
    this(name, namespace, provider);
    this.batchSize = batchSize;
  }

  /**
   * @param name       Unique name of the observer.
   * @param namespace  Namespace to use in CloudWatch.
   * @param cloudWatch AWS cloudwatch.
   */
  public CloudWatchMetricObserver(String name, String namespace, AmazonCloudWatch cloudWatch) {
    super(name);
    this.cloudWatch = cloudWatch;
    this.cloudWatchNamespace = namespace;
    batchSize = 20;
  }

  /**
   * @param name       Unique name of the observer.
   * @param namespace  Namespace to use in CloudWatch.
   * @param cloudWatch AWS cloudwatch.
   * @param batchSize  Batch size to send to Amazon.  They currently enforce a max of 20.
   */
  public CloudWatchMetricObserver(String name, String namespace, AmazonCloudWatch cloudWatch,
                                  int batchSize) {
    this(name, namespace, cloudWatch);
    this.batchSize = batchSize;
  }

  /**
   * @param metrics The list of metrics you want to send to CloudWatch
   */
  @Override
  public void updateImpl(List<Metric> metrics) {
    Preconditions.checkNotNull(metrics, "metrics");

    List<Metric> batch = new ArrayList<>(batchSize);

    for (final Metric m : metrics) {
      if (m.hasNumberValue()) {
        batch.add(m);

        if (batch.size() % batchSize == 0) {
          putMetricData(batch);
          batch.clear();
        }
      }
    }

    if (!batch.isEmpty()) {
      putMetricData(batch);
    }
  }

  private void putMetricData(List<Metric> batch) {
    METRICS_COUNTER.increment(batch.size());
    final Stopwatch s = PUTS_TIMER.start();
    try {
      cloudWatch.putMetricData(createPutRequest(batch));
    } catch (AmazonServiceException e) {
      final Tag error = new BasicTag("error", e.getErrorCode());
      DynamicCounter.increment(ERRORS_COUNTER_ID.withAdditionalTag(error));
      LOG.error("Error while submitting data for metrics : " + batch, e);
    } catch (Exception e) {
      final Tag error = new BasicTag("error", e.getClass().getSimpleName());
      DynamicCounter.increment(ERRORS_COUNTER_ID.withAdditionalTag(error));
      LOG.error("Error while submitting data for metrics : " + batch, e);
    } catch (Error e) {
      final Tag error = new BasicTag("error", e.getClass().getSimpleName());
      DynamicCounter.increment(ERRORS_COUNTER_ID.withAdditionalTag(error));
      throw Throwables.propagate(e);
    } finally {
      s.stop();
    }
  }

  PutMetricDataRequest createPutRequest(List<Metric> batch) {
    List<MetricDatum> datumList = batch.stream().map(this::createMetricDatum)
        .collect(Collectors.toList());

    return new PutMetricDataRequest().withNamespace(cloudWatchNamespace)
        .withMetricData(datumList);
  }

  MetricDatum createMetricDatum(Metric metric) {
    MetricDatum metricDatum = new MetricDatum();

    return metricDatum.withMetricName(metric.getConfig().getName())
        .withDimensions(createDimensions(metric.getConfig().getTags()))
        .withUnit("None")//DataSourceTypeToAwsUnit.getUnit(metric.))
        .withTimestamp(new Date(metric.getTimestamp()))
        .withValue(truncate(metric.getNumberValue()));
    //TODO Need to convert into reasonable units based on DataType
  }

  /**
   * Adjust a double value so it can be successfully written to cloudwatch. This involves capping
   * values with large exponents to an experimentally determined max value and converting values
   * with large negative exponents to 0. In addition, NaN values will be converted to 0.
   */
  Double truncate(Number numberValue) {
    // http://docs.amazonwebservices.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html
    double doubleValue = numberValue.doubleValue();
    if (truncateEnabled) {
      final int exponent = Math.getExponent(doubleValue);
      if (Double.isNaN(doubleValue)) {
        doubleValue = 0.0;
      } else if (exponent >= MAX_EXPONENT) {
        doubleValue = (doubleValue < 0.0) ? -MAX_VALUE : MAX_VALUE;
      } else if (exponent <= MIN_EXPONENT) {
        doubleValue = 0.0;
      }
    }
    return doubleValue;
  }

  List<Dimension> createDimensions(TagList tags) {
    List<Dimension> dimensionList = new ArrayList<>(tags.size());

    for (Tag tag : tags) {
      dimensionList.add(new Dimension().withName(tag.getKey()).withValue(tag.getValue()));
    }

    return dimensionList;
  }

  public CloudWatchMetricObserver withTruncateEnabled(boolean truncateEnabled) {
    this.truncateEnabled = truncateEnabled;
    return this;
  }
}
