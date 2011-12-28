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

package com.netflix.servo.monitoring;

/**
 * Defines rdd data source types which are supported by EPIC.
 *
 * @author gkim
 */
public enum DataSourceType {
    /**
     * GAUGE does not save the rate of change. It saves the actual value itself.
     * There are no divisions or calculations. Memory consumption in a server
     * is a typical example of gauge.
     */
    GAUGE,

    /**
     * COUNTER will save the rate of change of the value over a step period.
     * This assumes that the value is always increasing (the difference between
     * the current and the previous value is greater than 0). Traffic counters
     * on a router are an ideal candidate for using COUNTER
     */
    COUNTER,

    /**
     * DERIVE is the same as COUNTER, but it allows negative values as well. If
     * you want to see the rate of change in free diskspace on your server, then
     * you might want to use the DERIVE data type.
     */
    DERIVE,

    /**
     * Not part of RRD DST, but useful for status (Up/Down) monitoring
     */
    BOOLEAN,

    /**
     * Not part of RRD DST, but useful for debugging.  This will not be monitoring
     * by the NOC.
     */
    INFORMATIONAL;
}
