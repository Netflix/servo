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
package com.netflix.servo.tag.aws;

import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;

import com.google.common.io.Closeables;

import com.netflix.servo.aws.AwsPropertyKeys;
import com.netflix.servo.tag.Tag;

import com.netflix.servo.aws.constants.Dimensions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;

/**
 *
 * Group of Tags who's values will be dynamically set at runtime
 * based on amazon api calls.
 *
 * The keys for and values of these Tags are consistent with AWS naming.
 */
public enum AwsInjectableTag implements Tag {
    AUTOSCALE_GROUP(Dimensions.AUTOSCALING_GROUP.getAwsString(), getAutoScaleGroup()),
    INSTANCE_ID(Dimensions.INSTANCE_ID.getAwsString(), getInstanceId()),
    AVAILABILITY_ZONE(Dimensions.AVAILABILITY_ZONE.getAwsString(), getZone()),
    AMI_ID(Dimensions.AMI_IMAGE_ID.getAwsString(), getAmiId()),
    INSTANCE_TYPE(Dimensions.INSTANCE_TYPE.getAwsString(), getInstanceType()),
    LOCAL_HOSTNAME("local-hostname", getLocalHostname()),
    LOCAL_IPV4("local-ipv4", getLocalIpv4()),
    PUBLIC_HOSTNAME("public-hostname", getPublicHostname()),
    PUBLIC_IPV4("public-ipv4", getPublicIpv4());
    
    private static final String metaDataUrl = "http://169.254.169.254/latest/meta-data";
    private static final String undefined = "undefined";
    private static final Logger log = LoggerFactory.getLogger(AwsInjectableTag.class);

    private final String key;
    private final String value;

    private AwsInjectableTag(String key, String val) {
        this.key = key;
        this.value = val;
    }

    /**
     *
     * @return Amazon compliant string representation of the key.
     */
    public String getKey() {
        return key;
    }

    /**
     *
     * @return value as determined at runtime for the key.
     */
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
        BufferedReader reader = null;
        try {
            URL url = new URL(metaDataUrl + path);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line  = null;
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
        } finally {
            Closeables.closeQuietly(reader);
        }
    }

    static String getZone() {
        return getUrlValue("/placement/availability-zone");
    }
    
    static String getAmiId(){
        return getUrlValue("/ami-id");
    }

    static String getInstanceType(){
        return getUrlValue("/instance-type");
    }
    
    static String getLocalHostname(){
        return getUrlValue("/local-hostname");
    }
    
    static String getLocalIpv4(){
        return getUrlValue("/local-ipv4");
    }

    static String getPublicHostname(){
        return getUrlValue("/public-hostname");
    }
    
    static String getPublicIpv4(){
        return getUrlValue("/public-ipv4");
    }
}
