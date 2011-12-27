package com.netflix.monitoring;

public interface IMetricWriter {
    void write(Metric metric);
    void close();
}
