package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.Timer;

public class TimerBridge implements MetricBridge {
    public TimerBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    @Monitor(name="min", type= DataSourceType.GAUGE)
    private double min;
    @Monitor(name="max", type= DataSourceType.GAUGE)
    private double max;
    @Monitor(name="mean", type= DataSourceType.GAUGE)
    private double mean;
    @Monitor(name="median", type= DataSourceType.GAUGE)
    private double median;
    @Monitor(name="stddev", type= DataSourceType.GAUGE)
    private double stddev;
    @Monitor(name="95", type= DataSourceType.GAUGE)
    private double percentile95;
    @Monitor(name="99", type= DataSourceType.GAUGE)
    private double percentile99;
    @Monitor(name="99.9", type= DataSourceType.GAUGE)
    private double percentile999;

    @Override
    public void update(Metric metric) {
        Timer timer = (Timer) metric;
        min = timer.min();
        max = timer.max();
        mean = timer.mean();
        median = timer.getSnapshot().getMedian();
        stddev = timer.stdDev();
        percentile95 = timer.getSnapshot().get95thPercentile();
        percentile99 = timer.getSnapshot().get99thPercentile();
        percentile999 = timer.getSnapshot().get999thPercentile();
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public double getMean() {
        return mean;
    }

    public double getMedian() {
        return median;
    }

    public double getStddev() {
        return stddev;
    }

    public double getPercentile95() {
        return percentile95;
    }

    public double getPercentile99() {
        return percentile99;
    }

    public double getPercentile999() {
        return percentile999;
    }
}
