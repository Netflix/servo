package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

public class MeterBridge implements MetricBridge {
    public MeterBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    public void update(Metric metric) {
        Meter meter = (Meter) metric;

        count = meter.count();
        oneMinuteRate = meter.oneMinuteRate();
        meanRate = meter.meanRate();
        fiveMinuteRate = meter.fiveMinuteRate();
        fifteenMinuteRate = meter.fifteenMinuteRate();
    }

    @Monitor(name="count", type= DataSourceType.COUNTER)
    private long count;
    @Monitor(name="oneMinuteRate", type= DataSourceType.GAUGE)
    private double oneMinuteRate;
    @Monitor(name="meanRate", type= DataSourceType.GAUGE)
    private double meanRate;
    @Monitor(name="fiveMinuteRate", type= DataSourceType.GAUGE)
    private double fiveMinuteRate;
    @Monitor(name="fifteenMinuteRate", type= DataSourceType.GAUGE)
    private double fifteenMinuteRate;

    public long getCount() {
        return count;
    }

    public double getOneMinuteRate() {
        return oneMinuteRate;
    }

    public double getMeanRate() {
        return meanRate;
    }

    public double getFiveMinuteRate() {
        return fiveMinuteRate;
    }

    public double getFifteenMinuteRate() {
        return fifteenMinuteRate;
    }
}
