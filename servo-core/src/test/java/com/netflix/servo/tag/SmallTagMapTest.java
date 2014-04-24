/**
 * Copyright 2014 Netflix, Inc.
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.testng.Assert.*;

public class SmallTagMapTest {
    @Test
    public void testEmpty() {
        SmallTagMap smallTagMap = SmallTagMap.builder().result();
        assertTrue(smallTagMap.isEmpty());

        SmallTagMap notEmpty = SmallTagMap.builder().add(new BasicTag("k", "v")).result();
        assertFalse(notEmpty.isEmpty());
        assertEquals(notEmpty.size(), 1);
    }

    @Test
    public void testGet() {
        Tag tag = new BasicTag("k1", "v1");
        SmallTagMap map = SmallTagMap.builder().add(tag).result();

        assertEquals(map.get("k1"), tag);
        assertNull(map.get("k2"));
    }

    @Test
    public void testBuilderUpdatesExisting() {
        Tag t1 = new BasicTag("k1", "v1");
        Tag t2 = new BasicTag("k1", "v2");

        SmallTagMap map = SmallTagMap.builder().add(t1).add(t2).result();
        assertEquals(map.get("k1"), t2);
    }

    @Test
    public void testBuilderAddAll() {
        Tag t1 = new BasicTag("k1", "v1");
        Tag t2 = new BasicTag("k1", "v2");
        Tag t3 = new BasicTag("k2", "v2");
        List<Tag> tags = ImmutableList.of(t1, t2, t3);
        SmallTagMap map = SmallTagMap.builder().addAll(tags).result();
        assertEquals(map.get("k1"), t2);
        assertEquals(map.size(), 2);
    }

    @Test
    public void testIteratorEmpty() {
        SmallTagMap empty = SmallTagMap.builder().result();
        assertFalse(empty.iterator().hasNext());
    }

    @Test(expectedExceptions = NoSuchElementException.class)
    public void testIteratorNextThrows() {
        SmallTagMap empty = SmallTagMap.builder().result();
        empty.iterator().next();
    }

    @Test
    public void testIterator() {
        final Tag t1 = new BasicTag("k1", "v");
        final Tag t2 = new BasicTag("k2", "v2");
        SmallTagMap map = SmallTagMap.builder().add(t1).add(t2).result();
        Set<Tag> tags = ImmutableSet.copyOf(map.iterator());
        assertEquals(tags, ImmutableSet.of(t1, t2));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testTooManyTags() {
        SmallTagMap.Builder builder = SmallTagMap.builder();
        for (int i = 0; i <= SmallTagMap.MAX_TAGS; ++i) {
            builder.add(new BasicTag("k" + i, "0"));
        }
    }

    @Test
    public void testContains() {
        SmallTagMap map = SmallTagMap.builder().add(new BasicTag("k1", "v")).add(new BasicTag("k2", "v2")).result();
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertFalse(map.containsKey("k3"));
    }

    @Test
    public void testHashcode() {
        SmallTagMap map1 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).result();
        SmallTagMap map2 = SmallTagMap.builder().add(new BasicTag("k1", "v2")).result();
        SmallTagMap map3 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).result();

        assertEquals(map1.hashCode(), map1.hashCode());
        assertEquals(map1.hashCode(), map3.hashCode());
        assertNotEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEquals() {
        SmallTagMap map1 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).result();
        SmallTagMap map2 = SmallTagMap.builder().add(new BasicTag("k1", "v2")).result();
        SmallTagMap map3 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).result();
        SmallTagMap map4 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).add(new BasicTag("k2", "v2")).result();

        assertTrue(map1.equals(map1));
        assertTrue(map1.equals(map3));
        assertFalse(map1.equals(map2));
        assertFalse(map1.equals(null));
        assertFalse(map1.equals(map4));
    }

    @Test
    public void testToString() {
        SmallTagMap empty = SmallTagMap.builder().result();
        assertEquals(empty.toString(), "SmallTagMap{}");
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testIteratorImmutable() {
        SmallTagMap map1 = SmallTagMap.builder().add(new BasicTag("k1", "v1")).result();
        Iterator<Tag> it = map1.iterator();
        assertTrue(it.hasNext());
        it.remove();
    }
}
