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

/**
 * Used to indicate a monitor that should be reset after sampling. If there
 * are multiple pollers reading the value only one of them should be configured
 * to reset the value. The {@link Monitor#getValue()} call will not reset
 * the value so it can be sample many times.
 */
public interface ResettableMonitor<T> extends Monitor<T> {
    /** Return the value and reset the monitor state for the next polling interval. */
    T getAndResetValue();
}
