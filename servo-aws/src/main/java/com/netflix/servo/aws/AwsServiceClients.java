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
package com.netflix.servo.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;

/**
 * Static helpers for constructing configured AWS service clients
 */
public class AwsServiceClients {

  /**
   * Get a CloudWatch client whose endpoint is configured based on properties
   */
  public static AmazonCloudWatch cloudWatch(AWSCredentialsProvider credentials ) {
    AmazonCloudWatch client = new AmazonCloudWatchClient(credentials);
    client.setEndpoint( System.getProperty( AwsPropertyKeys.awsCloudWatchEndpoint, "monitoring.amazonaws.com" ) );
    return client;
  }

  /**
   * Get an AutoScaling client whose endpoint is configured based on properties
   */
  public static AmazonAutoScaling autoScaling(AWSCredentials credentials) {
    AmazonAutoScaling client = new AmazonAutoScalingClient(credentials);
    client.setEndpoint( System.getProperty( AwsPropertyKeys.awsAutoScalingEndpoint, "autoscaling.amazonaws.com" ) );
    return client;
  }
}
