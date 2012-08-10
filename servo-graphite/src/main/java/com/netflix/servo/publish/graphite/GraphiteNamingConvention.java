package com.netflix.servo.publish.graphite;

import com.netflix.servo.Metric;

/**
 * We want to allow the user to override the default graphite naming convention to massage the objects into
 * the right shape for their graphite setup. Naming conventions could also be applied to other observers
 * such as the file observer in the future.
 */
public interface GraphiteNamingConvention {

    String getName(Metric metric);
}
