/*
 * Copyright (c) 2011. Netflix, Inc.
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

package com.netflix.servo.monitoring.aws;

import com.netflix.servo.monitoring.Tag;

/**
 * User: gorzell
 * Date: 12/27/11
 * Time: 5:47 PM
 */
public enum AwsInjectableTag implements Tag {
    AUTOSCALE_GROUP("autoScalingGroup", getAutoScaleGroup()),
    INSTANCE_ID("instanceId", getInstanceId());

    private final String key;
    private final String value;

    private AwsInjectableTag(String key, String val) {
        this.key = key;
        this.value = val;
    }
    
    public String getKey(){
        return key;
    }
    
    public String getValue(){
        return value;
    }

    private static String getAutoScaleGroup() {
        return "";
    }

    private static String getInstanceId() {
        return "";
    }

}
