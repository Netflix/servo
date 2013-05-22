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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BasicTagTest {
    private static final String TEST_KEY = "foo";
    private static final String TEST_VALUE = "bar";
    private final BasicTag testTag = new BasicTag(TEST_KEY, TEST_VALUE);

    @Test
    public void testEquals() throws Exception {
        BasicTag localTag = new BasicTag(TEST_KEY, TEST_VALUE);
        BasicTag notEqualTag = new BasicTag(TEST_KEY, "goo");

        assertTrue(testTag != localTag);
        assertTrue(testTag.equals(localTag));
        assertTrue(testTag.getKey().equals(TEST_KEY));
        assertTrue(testTag.getValue().equals(TEST_VALUE));
        assertTrue(!testTag.equals(notEqualTag));
    }

    @Test
    public void testGetKey() throws Exception {
        assertEquals(testTag.getKey(), TEST_KEY);
    }

    @Test
    public void testGetValue() throws Exception {
        assertEquals(testTag.getValue(), TEST_VALUE);
    }

    @Test
    public void testParseTagValid() throws Exception {
        String goodString = "foo=bar";

        Tag t = Tags.parseTag(goodString);
        assertTrue(t.equals(testTag));

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseTagNoEqSign() throws Exception {
        String badString = "foobar";
        Tags.parseTag(badString);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseTagEmptyValue() throws Exception {
        String badString = "foo=";
        Tags.parseTag(badString);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testParseTagEmptyKey() throws Exception {
        String badString = "=bar";
        Tags.parseTag(badString);
    }
}
