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

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.tag.Tag;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class ResettableCounterTest extends AbstractMonitorTest<ResettableCounter> {

    public ResettableCounter newInstance(String name) {
        return new ResettableCounter(MonitorConfig.builder(name).build());
    }

    @Test
    public void testHasRightType() throws Exception {
        Tag type = newInstance("foo").getConfig().getTags().getTag(DataSourceType.KEY);
        assertEquals(type.getValue(), "GAUGE");
    }
}
