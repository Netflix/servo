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

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Utility class for dealing with concurrent types.
 */
final class AtomicUtils {
    private AtomicUtils() {
    }

    /**
     * AtomicLongArray doesn't override equals, so this is needed to do a comparison of ResettableMonitors.
     */
    static boolean equals(AtomicLongArray a, AtomicLongArray b) {
        if (a.length() != b.length()) {
            return false;
        }

        for (int i = 0; i < a.length(); ++i) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Provide a sane hashCode() for AtomicLongArrays.
     */
    static int hashCode(AtomicLongArray a) {
        int result = 1;

        for (int i = 0; i < a.length(); ++i) {
            long value = a.get(i);
            int hashCode = (int) (value ^ (value >>> 32));
            result = 31 * result + hashCode;
        }

        return result;
    }
}
