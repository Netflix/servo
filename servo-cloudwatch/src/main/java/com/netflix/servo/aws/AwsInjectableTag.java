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
package com.netflix.servo.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.netflix.servo.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * User: gorzell
 * Date: 12/27/11
 * Time: 5:47 PM
 */
public enum AwsInjectableTag implements Tag {
    AUTOSCALE_GROUP("autoScalingGroup", getAutoScaleGroup()),
    INSTANCE_ID("instanceId", getInstanceId()),
    EC2_ZONE("zone", getZone());

    private static final String metaDataUrl = "http://169.254.169.254/latest/meta-data";
    private static final String undefined = "undefined";
    private static final Logger log = LoggerFactory.getLogger(AwsInjectableTag.class);

    private final String key;
    private final String value;

    private AwsInjectableTag(String key, String val) {
        this.key = key;
        this.value = val;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    static String getAutoScaleGroup() {
        try {
            File credFile = new File(System.getProperties().getProperty(AwsPropertyKeys.awsCredentialsFile));
            AmazonAutoScaling autoScalingClient = new AmazonAutoScalingClient(new PropertiesCredentials(credFile));

            return autoScalingClient.describeAutoScalingInstances(
                    new DescribeAutoScalingInstancesRequest().withInstanceIds(getInstanceId()))
                    .getAutoScalingInstances().get(0).getAutoScalingGroupName();
        } catch (Exception e) {
            log.error("Unable to get ASG name.", e);
            return undefined;
        }
    }

    static String getInstanceId() {
        return getUrlValue("/instance-id");
    }

    static String getUrlValue(String path) {
        try {
            URL url = new URL(metaDataUrl + path);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            StringBuilder stringBuilder = new StringBuilder();
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.warn("", e);
            return undefined;
        }
    }
    
    static String getZone(){
        return undefined;
    }

}
