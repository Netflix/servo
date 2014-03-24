package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

public class GaugeLongBridge implements MetricBridge {
    public GaugeLongBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    @Monitor(name="gauge", type= DataSourceType.GAUGE)
    private long value;

    @Override
    public void update(Metric metric) {
        value = (Long) ((Gauge) metric).value();
    }
}
