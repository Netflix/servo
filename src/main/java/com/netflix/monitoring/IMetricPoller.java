package com.netflix.monitoring;

public interface IMetricPoller {
    void poll(IMetricFilter filter, IMetricWriter writer);
}
