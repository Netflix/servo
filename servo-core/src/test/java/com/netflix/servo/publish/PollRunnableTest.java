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

import com.netflix.servo.DefaultMonitorRegistry;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class PollRunnableTest {
/*    @Test
    public void testRun() throws Exception {
        BasicCounter o1 = new BasicCounter("one");
        o1.increment();
        DefaultMonitorRegistry.getInstance().registerAnnotatedObject(o1);

        List<MetricObserver> observerList = new ArrayList<MetricObserver>(1);
        MemoryMetricObserver observer = new MemoryMetricObserver("test", 3);
        observerList.add(observer);

        MonitorRegistryMetricPoller poller = new MonitorRegistryMetricPoller(DefaultMonitorRegistry.getInstance());

        PollRunnable runnable = new PollRunnable(poller, BasicMetricFilter.MATCH_ALL, observerList);
        runnable.run();

        assertTrue(observer.getObservations().size() == 1);
        assertTrue(observer.getObservations().get(0).get(0).getValue().intValue() == 1);
    }*/
}
