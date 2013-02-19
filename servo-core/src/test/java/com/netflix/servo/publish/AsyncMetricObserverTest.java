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
import static org.testng.Assert.assertTrue;

public class AsyncMetricObserverTest {

    private List<Metric> mkList(int v) {
        return ImmutableList.of(new Metric("m", SortedTagList.EMPTY, 0L, v));
    }

    @Test
    public void testUpdate() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("mem", 50);
        AsyncMetricObserver amo = new AsyncMetricObserver("async", mmo, 50);
        amo.update(mkList(1));
        amo.update(mkList(2));
        amo.update(mkList(3));
        amo.update(mkList(4));
        Thread.sleep(1000);
        assertEquals(mmo.getObservations(),
            ImmutableList.of(mkList(1), mkList(2), mkList(3), mkList(4)));
        amo.stop();
    }

    @Test
    public void testExceedQueueSize() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("mem", 50);
        MetricObserver smo = new SlowMetricObserver(mmo, 500L);
        AsyncMetricObserver amo = new AsyncMetricObserver("async", smo, 2);
        amo.update(mkList(1));
        amo.update(mkList(2));
        amo.update(mkList(3));
        amo.update(mkList(4));
        Thread.sleep(4000);

        // We should always get at least queueSize updates
        List<List<Metric>> observations = mmo.getObservations();
        assertTrue(observations.size() >= 2);

        // Older entries are overwritten, the last queueSize updates should
        // be in the output
        int last = observations.size() - 1;
        assertEquals(observations.get(last - 1), mkList(3));
        assertEquals(observations.get(last), mkList(4));
        amo.stop();
    }

    @Test
    public void testExpiration() throws Exception {
        MemoryMetricObserver mmo = new MemoryMetricObserver("mem", 50);
        MetricObserver smo = new SlowMetricObserver(mmo, 500L);
        AsyncMetricObserver amo = new AsyncMetricObserver("async", smo, 50, 250);
        amo.update(mkList(1));
        amo.update(mkList(2));
        amo.update(mkList(3));
        amo.update(mkList(4));
        Thread.sleep(4000);

        // Only the first update should have made it on time
        List<List<Metric>> observations = mmo.getObservations();
        assertEquals(observations.size(), 1);
        assertEquals(observations.get(0), mkList(1));
        amo.stop();
    }

    @Test
    public void testFailedUpdate() throws Exception {
        // Just making sure exception does not propagate
        MetricObserver fmo = new FailingMetricObserver();
        AsyncMetricObserver amo = new AsyncMetricObserver("async", fmo, 50, 250);
        amo.update(mkList(1));
        amo.update(mkList(1));
        amo.update(mkList(1));
        amo.update(mkList(1));
        amo.update(mkList(1));
        amo.update(mkList(1));
        Thread.sleep(1000);
        amo.stop();
    }
}
