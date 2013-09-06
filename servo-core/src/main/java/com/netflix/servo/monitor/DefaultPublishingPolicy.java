/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.monitor;

/**
 * The default publishing policy. Observers must follow the default behaviour when the
 * {@link MonitorConfig} associated with a {@link Monitor} uses this policy.
 */
public final class DefaultPublishingPolicy implements PublishingPolicy {
    private static final DefaultPublishingPolicy INSTANCE = new DefaultPublishingPolicy();

    private DefaultPublishingPolicy() { }

    public static DefaultPublishingPolicy getInstance() {
        return INSTANCE;
    }

    @Override
    public String toString() {
        return "DefaultPublishingPolicy";
    }
}
