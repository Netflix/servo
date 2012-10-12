package com.netflix.servo.monitor;

/**
 * The default publishing policy. Observers must follow the default behaviour when the {@link MonitorConfig}
 * associated with a {@link Monitor} uses this policy.
 */
public class DefaultPublishingPolicy implements PublishingPolicy {
    private static DefaultPublishingPolicy INSTANCE = new DefaultPublishingPolicy();
    private DefaultPublishingPolicy() {}

    public static DefaultPublishingPolicy getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "DefaultPublishingPolicy";
    }
}
