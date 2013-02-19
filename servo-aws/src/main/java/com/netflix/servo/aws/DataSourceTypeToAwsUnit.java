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

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.netflix.servo.annotations.DataSourceType;

/**
 * Conversion from internal data types to Amazon Units.
 */
public class DataSourceTypeToAwsUnit {
    public static String getUnit(DataSourceType dataSourceType){
        switch (dataSourceType){
            case COUNTER:
                return StandardUnit.CountSecond.toString();
            case GAUGE:
                return StandardUnit.None.toString();
            case INFORMATIONAL:
                return StandardUnit.None.toString();
            default:
                return StandardUnit.None.toString();
        }
    }
}
