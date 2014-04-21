/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.servo.util;

/**
 * A {@link Clock} that provides a way to modify the time returned by {@link System#currentTimeMillis()}.
 *
 * This can be used during application shutdown to force the clock forward and get the latest values which normally
 * would not be returned until the next step boundary is crossed.
 */
public enum ClockWithOffset implements Clock {
    INSTANCE;

    private volatile long offset = 0L;

    /**
     * Sets the offset for the clock.
     * @param offset Number of milliseconds to add to the current time.
     */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Override
    public long now() {
        return offset + System.currentTimeMillis();
    }
}
