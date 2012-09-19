package com.netflix.servo.examples;

import com.netflix.servo.annotations.DataSourceType;
import com.netflix.servo.monitor.Monitor;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.StandardTagKeys;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.Tags;

public class DynamicExample implements Monitor {
    protected MonitorConfig config;
    private long metricValue = 0;
    private long timestamp = -1;

    public DynamicExample(String id) {
        this.config = MonitorConfig.builder(id).withTag(DataSourceType.GAUGE).build();
    }
    public synchronized void setMetricValue(long metricValue) {
        setMetricValue(metricValue, -1);
    }

    public synchronized void setMetricValue(long metricValue, long timestamp) {
        this.metricValue = metricValue;
        this.timestamp = timestamp;
        if (timestamp != -1) {
            Tag timestampTag = Tags.newTag(StandardTagKeys.TIMESTAMP.getKeyName(), String.valueOf(timestamp));
            this.config = this.config.withAdditionalTag(timestampTag);
        }
    }

    @Override
    public Object getValue() {
        return this.metricValue;
    }

    @Override
    public MonitorConfig getConfig() {
        return config;
    }
}
