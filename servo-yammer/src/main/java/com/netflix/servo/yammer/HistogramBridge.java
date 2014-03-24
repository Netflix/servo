package com.netflix.servo.yammer;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.annotations.Monitor;
import com.netflix.servo.monitor.Monitors;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

public class HistogramBridge implements MetricBridge {
    public HistogramBridge(MetricName name) {
        Monitors.registerObject(name.getName(), this);
    }

    @Monitor(name="max", type= DataSourceType.GAUGE)
    private double max;
    @Monitor(name="min", type= DataSourceType.GAUGE)
    private double min;
    @Monitor(name="mean", type= DataSourceType.GAUGE)
    private double mean;
    @Monitor(name="median", type= DataSourceType.GAUGE)
    private double median;
    @Monitor(name="stddev", type= DataSourceType.GAUGE)
    private double stddev;
    @Monitor(name="95", type= DataSourceType.GAUGE)
    private double percentile95th;
    @Monitor(name="99", type= DataSourceType.GAUGE)
    private double percentile99th;
    @Monitor(name="99.9", type= DataSourceType.GAUGE)
    private double percentile999th;

    @Override
    public void update(Metric metric) {
        Histogram histogram = (Histogram) metric;

        min = histogram.min();
        max = histogram.max();
        mean = histogram.mean();
        median = histogram.getSnapshot().getMedian();
        stddev = histogram.stdDev();
        percentile95th = histogram.getSnapshot().get95thPercentile();
        percentile99th = histogram.getSnapshot().get99thPercentile();
        percentile999th = histogram.getSnapshot().get999thPercentile();
    }
}
