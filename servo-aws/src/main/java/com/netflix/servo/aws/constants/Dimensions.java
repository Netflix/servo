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
package com.netflix.servo.aws.constants;

/**
 * Constants related to the AWS API, and what the labels they use for Dimensions across their services.
 *
 * http://docs.amazonwebservices.com/AmazonCloudWatch/latest/DeveloperGuide/CW_Support_For_AWS.html
 */
public enum Dimensions {
    //EC2
    AMI_IMAGE_ID("ImageId"),
    INSTANCE_ID("InstanceId"),
    INSTANCE_TYPE("InstanceType"),
    //EBS
    VOLUME_ID("VolumeId"),
    //RDS
    DB_INSTANCE_ID("DBInstanceIdentifier"),
    DB_CLASS("DatabaseClass"),
    ENGINE_NAME("EngineName"),
    //SNS
    TOPIC_NAME("TopicName"),
    //SQS
    QUEUE_NAME("QueueName"),
    //ASG  Also can filter EC2 metrics
    AUTOSCALING_GROUP("AutoScalingGroupName"),
    //ELB
    LOAD_BALANCER_NAME("LoadBalancerName"),
    AVAILABILITY_ZONE("AvailabilityZone");


    private String awsString;

    private Dimensions(String awsString){
        this.awsString = awsString;
    }

    public String getAwsString(){
        return awsString;
    }
}
