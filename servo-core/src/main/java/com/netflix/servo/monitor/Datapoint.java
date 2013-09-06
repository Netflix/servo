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
package com.netflix.servo.monitor;

import com.google.common.base.Objects;

/**
 * Tuple for a timestamp and value.
 */
final class Datapoint {

    static final Datapoint UNKNOWN = new Datapoint(0L, -1L);

    private final long timestamp;
    private final long value;

    Datapoint(long timestamp, long value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    boolean isUnknown() {
        return (timestamp == 0L);
    }

    long getTimestamp() {
        return timestamp;
    }

    long getValue() {
        return value;
    }

    public int hashCode() {
        return Objects.hashCode(timestamp, value);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Datapoint)) {
            return false;
        }
        Datapoint dp = (Datapoint) obj;
        return timestamp == dp.timestamp && value == dp.value;
    }

    public String toString() {
        return Objects.toStringHelper(this)
            .add("timestamp", timestamp)
            .add("value", value)
            .toString();
    }
}

