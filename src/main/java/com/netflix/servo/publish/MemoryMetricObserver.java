/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
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
package com.netflix.servo.publish;

import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the last N observations in-memory.
 */
public final class MemoryMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MemoryMetricObserver.class);

    private final List<Metric>[] mObservations;
    private int mNext;

    public MemoryMetricObserver() {
        this("unamed observer", 10);
    }

    @SuppressWarnings("unchecked")
    public MemoryMetricObserver(String name, int num) {
        super(name);
        mObservations = (List<Metric>[]) new List[num];
        mNext = 0;
    }

    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        mObservations[mNext] = metrics;
        mNext = (mNext + 1) % mObservations.length;
    }

    public List<List<Metric>> getObservations() {
        ImmutableList.Builder<List<Metric>> builder = ImmutableList.builder();
        int pos = mNext;
        for (int i = 0; i < mObservations.length; ++i) {
            if (mObservations[pos] != null) {
                builder.add(mObservations[pos]);
            }
            pos = (pos + 1) % mObservations.length;
        }
        return builder.build();
    }
}
