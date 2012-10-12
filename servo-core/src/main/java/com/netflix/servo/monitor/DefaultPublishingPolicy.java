package com.netflix.servo.monitor;

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
