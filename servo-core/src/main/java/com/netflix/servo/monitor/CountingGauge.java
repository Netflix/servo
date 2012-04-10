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

import java.util.concurrent.atomic.AtomicLong;

/**
 * User: gorzell
 * Date: 4/9/12
 * Time: 6:36 PM
 */
public class CountingGauge implements Gauge<Long> {
    private final AtomicLong count = new AtomicLong();

    public void decrement() {
        count.decrementAndGet();
    }

    public void decrement(int amount) {
        decrement((long) amount);
    }

    public void decrement(long amount) {
        count.addAndGet(0 - amount);
    }

    public void decrement(Long amount) {
        decrement(amount.longValue());
    }

    public void increment() {
        count.incrementAndGet();
    }

    public void increment(int amount) {
        increment((long)amount);
    }

    public void increment(long amount) {
        count.getAndAdd(amount);
    }

    public void increment(Long amount) {
        increment(amount.longValue());
    }

    @Override
    public Long getValue() {
        return count.get();
    }

    @Override
    public void setValue(Long value) {
        count.set(value.longValue());
    }
}
