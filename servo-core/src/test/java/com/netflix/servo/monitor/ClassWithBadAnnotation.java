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

import static com.netflix.servo.annotations.DataSourceType.*;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(value = "SS_SHOULD_BE_STATIC",
        justification = "Values used through reflection")
public class ClassWithBadAnnotation {

    @com.netflix.servo.annotations.Monitor(name = "badGauge", type = GAUGE)
    private final String badGauge = "foo";

    @com.netflix.servo.annotations.Monitor(name = "annoGauge", type = GAUGE)
    private final AtomicLong a1 = new AtomicLong(0L);

    @com.netflix.servo.annotations.Monitor(name = "annoCounter", type = COUNTER)
    public final AtomicLong a2 = new AtomicLong(0L);

    @com.netflix.servo.annotations.Monitor(name = "annoInfo", type = INFORMATIONAL)
    private String getInfo() {
        return "foo";
    }
}
