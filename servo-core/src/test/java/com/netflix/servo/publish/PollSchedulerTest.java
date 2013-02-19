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

import org.testng.annotations.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class PollSchedulerTest {
    @Test
    public void testGetInstance() throws Exception {
        PollScheduler p = PollScheduler.getInstance();
        assertNotNull(p);
    }

    @Test
    public void testStartNoArg() throws Exception {
        PollScheduler.getInstance().start();
        assertTrue(PollScheduler.getInstance().isStarted());
        PollScheduler.getInstance().stop();
    }

    @Test
    public void testStart() throws Exception {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(2);

        PollScheduler.getInstance().start(s);
        assertTrue(PollScheduler.getInstance().isStarted());

        //PollScheduler.getInstance().addPoller( ,60, TimeUnit.SECONDS);

        PollScheduler.getInstance().stop();
    }

    @Test
    public void testStop() throws Exception {
        ScheduledExecutorService s = Executors.newScheduledThreadPool(2);

        PollScheduler.getInstance().start(s);
        assertTrue(PollScheduler.getInstance().isStarted());

        PollScheduler.getInstance().stop();
        assertTrue(!PollScheduler.getInstance().isStarted());
        assertTrue(s.isShutdown());
    }
}
