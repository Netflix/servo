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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Mostly for testing, this clock must be explicitly set to a given value. Defaults to init.
 */
class ManualClock implements Clock {

    private final AtomicLong time;

    ManualClock(long init) {
        time = new AtomicLong(init);
    }

    void set(long t) {
        time.set(t);
    }

    public long now() {
        return time.get();
    }
}
