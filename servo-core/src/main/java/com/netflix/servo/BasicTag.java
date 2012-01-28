/*
 * #%L
 * servo-core
 * %%
 * Copyright (C) 2011 - 2012 Netflix
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
package com.netflix.servo;

import com.google.common.base.Objects;

/**
 * User: gorzell
 * Date: 1/7/12
 * Time: 10:12 PM
 */
public class BasicTag implements Tag {

    private final String key;
    private final String value;

    public BasicTag(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BasicTag) {
            BasicTag t = (BasicTag) o;
            return key.equals(t.getKey()) && value.equals(t.getValue());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(key, value);
    }

    @Override
    public String toString() {
        return key + "=" + value;
    }

    public static BasicTag parseTag(String tagString) throws IllegalArgumentException {
        String k, v;
        int eqIndex = tagString.indexOf("=");

        if (eqIndex < 0) throw new IllegalArgumentException("Key and value must be separated by '='");

        k = tagString.substring(0, eqIndex);
        v = tagString.substring(eqIndex + 1, tagString.length());

        if (k.length() == 0 || v.length() == 0) throw new IllegalArgumentException("Key or Value cannot be empty");

        return new BasicTag(k, v);
    }
}
