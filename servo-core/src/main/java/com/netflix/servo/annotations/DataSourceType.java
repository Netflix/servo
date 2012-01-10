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
package com.netflix.servo.annotations;

public enum DataSourceType {
    /**
     * GAUGE does not save the rate of change. The value measured at a point
     * in time is stored. Examples are: CPU, memory, and disk usage
     */
    GAUGE,

    /**
     * COUNTER will save the rate of change of the value over a step period.
     * This assumes that the value is always increasing (the difference between
     * the current and the previous value is greater than 0).
     */
    COUNTER,

    /**
     * Not part of RRD DST, but useful for debugging. This will not be monitoring
     * by the NOC.
     */
    INFORMATIONAL
}
