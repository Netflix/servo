package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

public class CounterBridge implements MetricBridge {
    public CounterBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    @Monitor(name="counter", type= DataSourceType.COUNTER)
    private long value;

    public long getValue() { return value; }

    @Override
    public void update(Metric metric) {
        value = ((Counter) metric).count();
    }
}
