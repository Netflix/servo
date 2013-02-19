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
import com.netflix.servo.tag.SortedTagList;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;

public class MemoryMetricObserverTest {

    private List<Metric> mkList(int v) {
        return ImmutableList.of(new Metric("m", SortedTagList.EMPTY, 0L, v));
    }

    @Test
    public void testUpdate() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("test", 2);
        mmo.update(mkList(1));
        assertEquals(mmo.getObservations(), ImmutableList.of(mkList(1)));
    }

    @Test
    public void testExceedN() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("test", 2);
        mmo.update(mkList(1));
        mmo.update(mkList(2));
        mmo.update(mkList(3));
        assertEquals(mmo.getObservations(),
            ImmutableList.of(mkList(2), mkList(3)));
    }
}
