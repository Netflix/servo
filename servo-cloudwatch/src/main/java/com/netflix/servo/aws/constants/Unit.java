/*
 * Copyright (c) 2012. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.netflix.servo.aws.constants;

/**
 * User: gorzell
 * Date: 1/10/12
 * Time: 9:47 AM
 */
public enum Unit {
    /*
    aws-java-sdk-1.2.10/documentation/javadoc/com/amazonaws/services/cloudwatch/model/MetricDatum.html#getUnit()
    Allowed Values: Seconds, Microseconds, Milliseconds, Bytes, Kilobytes,
    Megabytes, Gigabytes, Terabytes, Bits, Kilobits, Megabits, Gigabits,
    Terabits, Percent, Count, Bytes/Second, Kilobytes/Second, Megabytes/Second,
    Gigabytes/Second, Terabytes/Second, Bits/Second, Kilobits/Second, Megabits/Second,
    Gigabits/Second, Terabits/Second, Count/Second, None
     */
    SECONDS("Seconds"),
    MICROSECONDS("Microseconds"),
    MILLISECONDS("Milliseconds"),
    BYTES("Bytes"),
    KILOBYTES("Kilobytes"),
    MEGABYTES("Megabytes"),
    GIGABYTES("Gigabytes"),
    TERABYTES("Terabytes"),
    BITS("Bits"),
    KILOBITS("Kilobits"),
    MEGABITS("Megabits"),
    GIGABITS("Gigabits"),
    TERABITS("Terabits"),
    PERCENT("Percent"),
    COUNT("Count"),
    BYTES_SECOND("Bytes/Second"),
    KILOBYTES_SECOND("Kilobytes/Second"),
    MEGABYTES_SECOND("Megabytes/Second"),
    GIGABYTES_SECOND("Gigabytes/Second"),
    TERABYTES_SECOND("Terabytes/Second"),
    BITS_SECOND("Bits/Second"),
    KILOBITS_SECOND("Kilobits/Second"),
    MEGABITS_SECOND("Megabits/Second"),
    GIGABITS_SECOND("Gigabits/Second"),
    TERABITS_SECOND("Terabits/Second"),
    COUNT_SECOND("Count/Second"),
    None("None");

    private String awsString;

    private Unit(String awsString) {
        this.awsString = awsString;
    }

    public String awsString() {
        return awsString;
    }
}
