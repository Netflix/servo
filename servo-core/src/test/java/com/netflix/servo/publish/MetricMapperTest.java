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
package com.netflix.servo.publish;

import com.google.common.collect.ImmutableList;
import com.netflix.servo.Metric;
import com.netflix.servo.tag.BasicTagList;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class MetricMapperTest {
    @Test
    public void testIdentityMapper() {
        Metric m1 = new Metric("m1", BasicTagList.of("k", "v"), 0, 1.0);
        Metric m2 = new Metric("m2", BasicTagList.of("k", "v"), 0, 2.0);
        List<Metric> metricList = ImmutableList.of(m1, m2);

        assertEquals(IdentityMetricMapper.INSTANCE.map(metricList), metricList);
    }
}
