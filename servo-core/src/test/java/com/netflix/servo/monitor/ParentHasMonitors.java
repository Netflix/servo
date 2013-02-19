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

import static com.netflix.servo.annotations.DataSourceType.*;

import java.util.concurrent.atomic.AtomicLong;

public class ParentHasMonitors extends ClassWithMonitors {

    private final Counter c = Monitors.newCounter("myCounter");

    @com.netflix.servo.annotations.Monitor(name = "myGauge", type = GAUGE)
    private final AtomicLong a1 = new AtomicLong(0L);
}
