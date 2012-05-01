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

package com.netflix.servo.monitor;

import com.netflix.servo.Monitor;

import java.util.concurrent.TimeUnit;

/**
 * User: gorzell
 * Date: 4/10/12
 * Time: 7:49 PM
 */
public interface Timer extends Monitor<Long> {

    public TimeUnit getTimeUnit();

    public void record(long duration);

    public void record(long duration, TimeUnit timeUnit);
}
