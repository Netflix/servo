/**
 * Copyright 2014 Netflix, Inc.
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

import com.netflix.servo.DefaultMonitorRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Get, and register if needed, monitors from {@link MonitorConfig}s or names.
 */
public final class Servo {
    private static ConcurrentMap<MonitorConfig, Counter> counters =
            new ConcurrentHashMap<MonitorConfig, Counter>();
    private static ConcurrentMap<MonitorConfig, Timer> timers =
            new ConcurrentHashMap<MonitorConfig, Timer>();
    private static ConcurrentMap<MonitorConfig, NumberGauge> gauges =
            new ConcurrentHashMap<MonitorConfig, NumberGauge>();
    private static ConcurrentMap<MonitorConfig, BasicDistributionSummary> summaries =
            new ConcurrentHashMap<MonitorConfig, BasicDistributionSummary>();

    private Servo() {
    }

    /**
     * Get a {@link Counter} given a {@link MonitorConfig} registering it if needed.
     */
    public static Counter getCounter(MonitorConfig config) {
        Counter v = counters.get(config);
        if (v != null) {
            return v;
        } else {
            Counter counter = new BasicCounter(config);
            Counter prevCounter = counters.putIfAbsent(config, counter);
            if (prevCounter != null) {
                return prevCounter;
            } else {
                DefaultMonitorRegistry.getInstance().register(counter);
                return counter;
            }
        }
    }

    /**
     * Get a {@link Counter} given a name registering it if needed.
     */
    public static Counter getCounter(String name) {
        return getCounter(MonitorConfig.builder(name).build());
    }

    /**
     * Get a {@link Timer} given a {@link MonitorConfig} registering it if needed.
     */
    public static Timer getTimer(MonitorConfig config) {
        Timer v = timers.get(config);
        if (v != null) {
            return v;
        } else {
            Timer timer = new BasicTimer(config, TimeUnit.SECONDS);
            Timer prevTimer = timers.putIfAbsent(config, timer);
            if (prevTimer != null) {
                return prevTimer;
            } else {
                DefaultMonitorRegistry.getInstance().register(timer);
                return timer;
            }
        }
    }

    /**
     * Get a {@link Timer} given a name registering it if needed.
     */
    public static Timer getTimer(String name) {
        return getTimer(MonitorConfig.builder(name).build());
    }

    /**
     * Get and register a number gauge.
     *
     * @param config The {@link MonitorConfig} that defines the {@link Gauge}
     * @param number a thread-safe implementation of {@link Number} used to access the value.
     * @return a number that will become the value of the gauge.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Number> T getNumberGauge(MonitorConfig config, T number) {
        NumberGauge v = gauges.get(config);
        if (v != null) {
            return (T) v.getValue(0);
        } else {
            NumberGauge gauge = new NumberGauge(config, number);
            NumberGauge prev = gauges.putIfAbsent(config, gauge);
            if (prev != null) {
                return (T) prev.getValue(0);
            } else {
                DefaultMonitorRegistry.getInstance().register(gauge);
                return (T) gauge.getValue(0);
            }
        }
    }

    /**
     * Get and register a number gauge.
     *
     * @param name   The name that defines the {@link Gauge}
     * @param number a thread-safe implementation of {@link Number} used to access the value.
     * @return a number that will become the value of the gauge.
     */
    public static <T extends Number> T getNumberGauge(String name, T number) {
        return getNumberGauge(MonitorConfig.builder(name).build(), number);
    }

    /**
     * Get and register a {@link BasicDistributionSummary}.
     */
    public static BasicDistributionSummary getDistributionSummary(String name) {
        return getDistributionSummary(MonitorConfig.builder(name).build());
    }

    /**
     * Get and register a {@link BasicDistributionSummary}.
     */
    public static BasicDistributionSummary getDistributionSummary(MonitorConfig config) {
        BasicDistributionSummary v = summaries.get(config);
        if (v != null) {
            return v;
        } else {
            BasicDistributionSummary summary = new BasicDistributionSummary(config);
            BasicDistributionSummary prevSummary = summaries.putIfAbsent(config, summary);
            if (prevSummary != null) {
                return prevSummary;
            } else {
                DefaultMonitorRegistry.getInstance().register(summary);
                return summary;
            }
        }
    }
}
