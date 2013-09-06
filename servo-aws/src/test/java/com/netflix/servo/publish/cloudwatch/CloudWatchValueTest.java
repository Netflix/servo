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

import com.amazonaws.auth.*;
import com.amazonaws.services.cloudwatch.*;
import com.amazonaws.services.cloudwatch.model.*;

import java.util.*;

/**
 * Test program for exploring the limits for values that can be written to cloudwatch.
 *
 * <pre>
 * ERROR NaN 1024 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value ? for parameter MetricData.member.1.Value is invalid.
 * ERROR -Infinity 1024 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value -∞ for parameter MetricData.member.1.Value is invalid.
 * ERROR Infinity 1024 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value ∞ for parameter MetricData.member.1.Value is invalid.
 * ERROR 4.900000e-324 -1023 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value 0 for parameter MetricData.member.1.Value is invalid.
 * ERROR 1.797693e+308 1023 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value 179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 for parameter MetricData.member.1.Value is invalid.
 * ERROR 4.697085e+108 361 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value 4,697,085,165,547,666,500,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 for parameter MetricData.member.1.Value is invalid.
 * ERROR 2.128980e-109 -361 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value 0 for parameter MetricData.member.1.Value is invalid.
 * ERROR -4.697085e+108 361 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value -4,697,085,165,547,666,500,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000 for parameter MetricData.member.1.Value is invalid.
 * ERROR -2.128980e-109 -361 - com.amazonaws.services.cloudwatch.model.InvalidParameterValueException: The value -0 for parameter MetricData.member.1.Value is invalid.
 * </pre>
 */
public class CloudWatchValueTest {

    private static final String accessKey = "";
    private static final String secretKey = "";

    private static final AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
    private static final AmazonCloudWatch client = new AmazonCloudWatchClient(credentials);

    private static final double[] specialValues = {
        Double.NaN,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.MIN_VALUE,
        Double.MAX_VALUE,
        Math.pow(2.0, 360),
        -Math.pow(2.0, 360),
        0.0,
        1.0,
        -1.0
    };

    private static double[] getValues(double start, double multiplier, int n) {
        double[] values = new double[n];
        values[0] = start;
        for (int i = 1; i < n; ++i) {
            values[i] = values[i - 1] * multiplier;
        }
        return values;
    }

    private static boolean putValue(String name, long timestamp, double value) {
        Date d = new Date(timestamp);
        MetricDatum m = new MetricDatum().withMetricName(name).withTimestamp(d).withValue(value);
        PutMetricDataRequest req = new PutMetricDataRequest().withNamespace("TEST").withMetricData(m);
        try {
            client.putMetricData(req);
            return true;
        } catch (Exception e) {
            System.out.printf("ERROR %e %d - %s: %s%n",
                value, Math.getExponent(value), e.getClass().getName(), e.getMessage());
            return false;
        }
    }

    private static void putValues(String name, long start, double[] values, boolean ignoreFailures) {
        long t = start;
        boolean succeeded = true;
        for (int i = 0; (succeeded || ignoreFailures) && i < values.length; ++i, t += 60000) {
            succeeded = putValue(name, t, values[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: cwtest <test-name>");
            System.exit(1);
        }

        long start = System.currentTimeMillis() - (1000 * 60 * 1000);
        double[] posLargeValues = getValues(1.0, 2.0, 500);
        double[] posSmallValues = getValues(1.0, 0.5, 500);
        double[] negLargeValues = getValues(-1.0, 2.0, 500);
        double[] negSmallValues = getValues(-1.0, 0.5, 500);

        putValues(args[0] + "_special", start, specialValues, true);
        putValues(args[0] + "_pos_large", start, posLargeValues, false);
        putValues(args[0] + "_pos_small", start, posSmallValues, false);
        putValues(args[0] + "_neg_large", start, negLargeValues, false);
        putValues(args[0] + "_neg_small", start, negSmallValues, false);
    }
}

