/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

public class Metric {
    private final String mName;
    private final Map<String,String> mTags;
    private final long mTimestamp;
    private final Number mValue;

    public Metric(
            String name,
            Map<String,String> tags,
            long timestamp,
            Number value) {
        mName = Preconditions.checkNotNull(name, "name cannot be null");
        mTags = ImmutableMap.copyOf(tags);
        mTimestamp = timestamp;
        mValue = Preconditions.checkNotNull(
            value, "value cannot be null (name=%s, tags=%s)", mName, mTags);
    }

    public String name() {
        return mName;
    }

    public Map<String,String> tags() {
        return mTags;
    }

    public long timestamp() {
        return mTimestamp;
    }

    public Number value() {
        return mValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Metric)) {
            return false;
        }
        Metric m = (Metric) obj;
        return mName.equals(m.name())
            && mTags.equals(m.tags())
            && mTimestamp == m.timestamp()
            && mValue.equals(m.value());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(mName, mTags, mTimestamp, mValue);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", mName)
            .add("tags", mTags)
            .add("timestamp", mTimestamp)
            .add("value", mValue)
            .toString();
    }
}
