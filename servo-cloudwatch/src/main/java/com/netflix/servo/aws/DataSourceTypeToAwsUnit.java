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

package com.netflix.servo.aws;

import com.netflix.servo.annotations.DataSourceType;

/**
 * User: gorzell
 * Date: 1/9/12
 * Time: 5:31 PM
 */
public class DataSourceTypeToAwsUnit {
    private static final String defaultUnit = "None";
    private static final String count = "Count";
    private static final String countSecond = "Count/Second";

    public static String getUnit(DataSourceType dataSourceType){
        switch (dataSourceType){
            case COUNTER:
                return countSecond;
            case GAUGE:
                return defaultUnit;
            case INFORMATIONAL:
                return defaultUnit;
            default:
                return defaultUnit;
        }
    }
}
