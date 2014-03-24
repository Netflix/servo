package com.netflix.servo.yammer;

import com.yammer.metrics.core.Metric;

public interface MetricBridge {
    void update(Metric metric);
}
