package com.netflix.monitoring;

import java.util.Map;

public interface IMetricFilter {
    boolean matches(String name, Map<String,String> tags);
}
