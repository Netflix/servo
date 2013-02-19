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
 * Constants for the namespaces aws publish their metrics to cloudwatch under.
 */
public enum Namespace {
    AWS_EBS("AWS/EBS"),
    AWS_EC2("AWS/EC2"),
    AWS_RDS("AWS/RDS"),
    AWS_SQS("AWS/SQS"),
    AWS_SNS("AWS/SNS"),
    AWS_AUTOSCALING("AWS/AutoScaling"),
    AWS_ELB("AWS/ELB");

    private String value;

    private Namespace(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
