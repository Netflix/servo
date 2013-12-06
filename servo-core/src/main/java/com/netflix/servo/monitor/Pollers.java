/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.netflix.servo.monitor;

final class Pollers {
    private Pollers() {
    }

    /**
     * A comma separated list of longs indicating the frequency of the pollers. For example: <br/>
     * {@code 60000, 10000 }<br/>
     * indicates that the main poller runs every 60s and a secondary poller will run every 10 seconds.
     * This is used to deal with monitors that need to get reset after they're polled. For example a MinGauge
     * or a ResettableCounter.
     */
    static final String POLLERS = System.getProperty("servo.pollers", "60000");
    static final long[] POLLING_INTERVALS = parse(POLLERS);
    static int NUM_POLLERS = POLLING_INTERVALS.length;

    static long[] parse(String pollers) {
        String[] periods = pollers.split(",\\s*");
        long[] result = new long[periods.length];
        for (int i = 0; i < periods.length; ++i) {
            String period = periods[i];
            result[i] = Long.parseLong(period);
        }
        return result;
    }
}