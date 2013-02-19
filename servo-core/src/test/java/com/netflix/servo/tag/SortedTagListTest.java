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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class SortedTagListTest {

    static final Tag a = new BasicTag("a", "a");
    static final Tag b = new BasicTag("b", "b");
    static final Tag c = new BasicTag("c", "c");

    static final Tag[] tagArray = new Tag[3];

    public TagList testListFromStrings;
    public TagList testListFromCollection;
    public TagList testListFromTags;
    public Collection<Tag> collection;

    @BeforeClass
    public void setup() throws Exception {
        tagArray[0] = a;
        tagArray[1] = b;
        tagArray[2] = c;

        testListFromStrings = new SortedTagList.Builder().withTag("a", "a").withTag("b", "b").withTag("c", "c").build();

        collection = new ArrayList<Tag>();
        collection.add(a);
        collection.add(c);
        collection.add(b);

        testListFromCollection = new SortedTagList.Builder().withTags(collection).build();
        testListFromTags = new SortedTagList.Builder().withTag(c).withTag(a).withTag(b).build();
    }

    @Test
    public void testGetTag() throws Exception {
        assertTrue(testListFromCollection.getTag("a").equals(a));
        assertTrue(testListFromStrings.getTag("b").equals(b));
        assertTrue(testListFromTags.getTag("c").equals(c));
    }

    @Test
    public void testContainsKey() throws Exception {
        assertTrue(testListFromCollection.containsKey("b"));
        assertTrue(testListFromStrings.containsKey("c"));
        assertTrue(testListFromTags.containsKey("a"));
    }

    @Test
    public void testIsEmpty() throws Exception {
        assertTrue(SortedTagList.EMPTY.isEmpty());
        assertTrue(!testListFromCollection.isEmpty());
        assertTrue(!testListFromStrings.isEmpty());
        assertTrue(!testListFromTags.isEmpty());
    }

    @Test
    public void testSize() throws Exception {
        assertTrue(SortedTagList.EMPTY.isEmpty());
        assertTrue(!testListFromCollection.isEmpty());
        assertTrue(!testListFromStrings.isEmpty());
        assertTrue(!testListFromTags.isEmpty());
    }

    @Test
    public void testOrder() throws Exception {
        int i = 0;
        for (Tag testListFromString : testListFromStrings) {
            assertEquals(testListFromString, tagArray[i]);
            i++;
        }

        i = 0;
        for (Tag testListFromTag : testListFromTags) {
            assertEquals(testListFromTag, tagArray[i]);
            i++;
        }

        i = 0;
        for (Tag aTestListFromCollection : testListFromCollection) {
            assertEquals(aTestListFromCollection, tagArray[i]);
            i++;
        }
    }

    @Test
    public void testAsMap() throws Exception {
        Map<String, String> stringMap = testListFromCollection.asMap();

        int i = 0;
        for (String s : stringMap.keySet()){
            assertEquals(s, tagArray[i].getKey());
            i++;
        }

        i = 0;
        for (String s : stringMap.values()){
            assertEquals(s, tagArray[i].getValue());
            i++;
        }
    }
}
