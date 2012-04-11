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

import com.netflix.servo.MonitorContext;

import java.util.concurrent.TimeUnit;

/**
 * User: gorzell
 * Date: 4/10/12
 * Time: 9:03 PM
 * <p/>
 * This implementation is not thread safe.
 */
public class BasicTimer extends AbstractMonitor<Long> implements Timer {
    private final TimeUnit timeUnit;
    private long start;
    private long end;
    private long time = -1;
    private boolean started;

    public BasicTimer(MonitorContext context, TimeUnit unit) {
        super(context);
        this.timeUnit = unit;
    }

    @Override
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean start() {
        if (started) return false;

        start = System.nanoTime();
        started = true;
        return true;
    }

    @Override
    public boolean stop() {
        if (!started) return false;

        end = System.nanoTime();
        started = false;
        return true;
    }

    @Override
    public void record() {
        time = end - start;
    }

    @Override
    public void stopAndRecord() {
        stop();
        record();
    }

    @Override
    public Long getValue() {
        return Long.valueOf(time);
    }
}
