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
package com.netflix.servo.tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Group of Tags who's values will be dynamically set at runtime
 * based on local calls.
 */
public enum InjectableTag implements Tag {
    HOSTNAME("hostname", getHostName()),
    IP("ip", getIp());

    private static final Logger LOGGER = LoggerFactory.getLogger(InjectableTag.class);

    private final String key;
    private final String value;

    private InjectableTag(String key, String val) {
        this.key = key;
        this.value = val;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String tagString() {
        return key + "=" + value;
    }

    private static String getHostName() {
        return (loadAddress() != null) ? loadAddress().getHostName() : "unkownHost";
    }

    private static String getIp() {
        return (loadAddress() != null) ? loadAddress().getHostAddress() : "unknownHost";
    }

    private static InetAddress loadAddress() {
        try {
            return InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            LOGGER.warn("Unable to load INET info.", e);
            return null;
        }
    }
}
