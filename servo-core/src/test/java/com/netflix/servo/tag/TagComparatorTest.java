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

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class TagComparatorTest {
    private Tag a;
    private Tag b;
    private Tag aa;
    private Tag ab;
    private TagComparator comparator;

    @BeforeTest
    public void setupTest() throws Exception {
        comparator = new TagComparator();
        a = new BasicTag("a", "a");
        b = new BasicTag("b", "b");
        aa = new BasicTag("a", "a");
        ab = new BasicTag("a", "b");
    }

    @Test
    public void testCompareFirstLevel() throws Exception {
        assertTrue(comparator.compare(a, b) < 0);
        assertTrue(comparator.compare(b, a) > 0);

    }

    @Test
    public void testCompareSecondLevel() throws Exception {
        assertTrue(comparator.compare(aa, ab) < 0);
        assertTrue(comparator.compare(ab, aa) > 0);
    }

    @Test
    public void testCompareEqual() throws Exception {
        assertTrue(comparator.compare(a, aa) == 0);
    }
}
