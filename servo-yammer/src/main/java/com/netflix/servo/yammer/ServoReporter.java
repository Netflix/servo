package com.netflix.servo.yammer;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.*;
import com.yammer.metrics.reporting.AbstractPollingReporter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ServoReporter extends AbstractPollingReporter implements
        MetricProcessor<ServoReporter.Context> {

    public static void enable(long period, TimeUnit unit) {
        enable(Metrics.defaultRegistry(), period, unit);
    }

    public static void enable(MetricsRegistry metricsRegistry, long period, TimeUnit unit) {
        final ServoReporter reporter = new ServoReporter(metricsRegistry);
        reporter.start(period, unit);
    }

    /**
     * The context used to output metrics.
     */
    public interface Context {
        MetricBridge getMonitor();
    }

    private final MetricPredicate predicate;
    private final Clock clock;
    private final Map<MetricName, MetricBridge> monitorMap;

    public ServoReporter(MetricsRegistry metricsRegistry) {
        this(metricsRegistry, MetricPredicate.ALL);
    }

    public ServoReporter(MetricsRegistry metricsRegistry,
                       MetricPredicate predicate) {
        this(metricsRegistry, predicate, Clock.defaultClock());
    }

    public ServoReporter(MetricsRegistry metricsRegistry,
                       MetricPredicate predicate,
                       Clock clock) {
        super(metricsRegistry, "servo-reporter");
        this.predicate = predicate;
        this.clock = clock;
        this.monitorMap = new HashMap<MetricName, MetricBridge>();
    }

    @Override
    public void run() {
        final Set<Map.Entry<MetricName, Metric>> metrics = getMetricsRegistry().allMetrics().entrySet();
        try {
            for (Map.Entry<MetricName, Metric> entry : metrics) {
                final MetricName metricName = entry.getKey();
                final Metric metric = entry.getValue();
                if (predicate.matches(metricName, metric)) {
                    final Context context = new Context() {
                        @Override
                        public MetricBridge getMonitor() {
                            MetricBridge monitor = monitorMap.get(metricName);
                            if (monitor == null) {
                                monitor = createMetric(metricName, metric);
                                monitorMap.put(metricName, monitor);
                            }
                            return monitor;
                        }

                    };
                    metric.processWith(this, entry.getKey(), context);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MetricBridge createMetric(MetricName name, Metric metric) {
        if (metric instanceof Meter) {
            return new MeterBridge(name);
        } else if (metric instanceof Histogram) {
            return new HistogramBridge(name);
        } else if (metric instanceof Counter) {
            return new CounterBridge(name);
        } else if (metric instanceof Timer) {
            return new TimerBridge(name);
        } else if (metric instanceof Gauge) {
            Object value = ((Gauge) metric).value();
            if (value instanceof Long) {
                return new GaugeLongBridge(name);
            } else if (value instanceof Integer) {
                return new GaugeIntBridge(name);
            } else if (value instanceof Double) {
                return new GaugeDoubleBridge(name);
            } else {
                throw new IllegalArgumentException("invalid gauge type: " + value);
            }
        } else {
            throw new IllegalArgumentException("invalid metric type: " + metric.getClass());
        }
    }

    @Override
    public void processMeter(MetricName name, Metered meter, Context context) throws IOException {
        context.getMonitor().update(meter);
    }

    @Override
    public void processCounter(MetricName name, Counter counter, Context context) throws IOException {
        context.getMonitor().update(counter);
    }

    @Override
    public void processHistogram(MetricName name, Histogram histogram, Context context) throws IOException {
        context.getMonitor().update(histogram);
    }

    @Override
    public void processTimer(MetricName name, Timer timer, Context context) throws IOException {
        context.getMonitor().update(timer);
    }

    @Override
    public void processGauge(MetricName name, Gauge<?> gauge, Context context) throws IOException {
        context.getMonitor().update(gauge);
    }

    @Override
    public void start(long period, TimeUnit unit) {
        super.start(period, unit);
    }

    @Override
    public void shutdown() {
        super.shutdown();
    }
}