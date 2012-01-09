/*
 * Copyright (c) 2012. Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.netflix.servo;

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

    @Override
    public boolean equals(Object o) {
        if (o instanceof BasicTag) {
            BasicTag t = (BasicTag) o;
            return key.equals(t.getKey()) && value.equals(t.getValue());
        } else {
            return false;
        }
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public static BasicTag parseTag(String tagString) throws IllegalArgumentException {
        String k, v;
        int eqIndex = tagString.indexOf("=");

        if (eqIndex < 0) throw new IllegalArgumentException("Key and value must be separated by '='");

        k = tagString.substring(0, eqIndex);
        v = tagString.substring(eqIndex + 1, tagString.length());

        return new BasicTag(k, v);
    }
}
