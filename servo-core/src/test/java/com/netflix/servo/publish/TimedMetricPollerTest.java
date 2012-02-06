/*
 * #%L
 * servo-core
 * %%
 * Copyright (C) 2011 - 2012 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

package com.netflix.servo.publish;

import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

public class TimedMetricPollerTest {
    @Test
    public void testGetInstance() throws Exception {
        TimedMetricPoller p = TimedMetricPoller.getInstance();
        assertNotNull(p);
    }

    @Test
    public void testStartNoArg() throws Exception {
        TimedMetricPoller.getInstance().start();
        assertTrue(TimedMetricPoller.getInstance().isStarted());
        TimedMetricPoller.getInstance().stop();
    }

    @Test
    public void testStart() throws Exception {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(2);
        
        TimedMetricPoller.getInstance().start(s);
        assertTrue(TimedMetricPoller.getInstance().isStarted());
        
        //TimedMetricPoller.getInstance().addPoller( ,60, TimeUnit.SECONDS);

        TimedMetricPoller.getInstance().stop();
    }

    @Test
    public void testStop() throws Exception {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(2);

        TimedMetricPoller.getInstance().start(s);
        assertTrue(TimedMetricPoller.getInstance().isStarted());
        
        TimedMetricPoller.getInstance().stop();
        assertTrue(!TimedMetricPoller.getInstance().isStarted());
        assertTrue(s.isShutdown());
    }
}
