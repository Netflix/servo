package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

public class GaugeIntBridge implements MetricBridge {
    public GaugeIntBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    @Monitor(name="gauge", type= DataSourceType.GAUGE)
    private int value;

    @Override
    public void update(Metric metric) {
        value = (Integer) ((Gauge) metric).value();
    }
}
