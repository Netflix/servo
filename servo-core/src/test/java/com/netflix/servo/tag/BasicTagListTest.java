/**
 * Copyright 2013 Netflix, Inc.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.servo.tag;

import com.netflix.servo.util.Preconditions;
import com.netflix.servo.util.UnmodifiableList;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class BasicTagListTest {
  static Map<String, String> mapOf(String... elts) {
    Preconditions.checkArgument(elts.length > 0, "elts must not be empty");
    Preconditions.checkArgument(elts.length % 2 == 0, "elts must be even: key,value pairs");
    Map<String, String> res = new HashMap<>(elts.length / 2);
    for (int i = 0; i < elts.length; i += 2) {
      final String key = elts[i];
      final String value = elts[i + 1];
      res.put(key, value);
    }
    return res;
  }

  @Test
  public void testCopyOfMap() throws Exception {
    Map<String, String> input = mapOf("foo", "bar", "dee", "dum");
    TagList tags = BasicTagList.copyOf(input);
    assertEquals(tags.asMap(), input);
  }

  @Test
  public void testCopyOfIterableString() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    List<String> input = UnmodifiableList.of("foo=bar", "dee=dum");
    TagList tags = BasicTagList.copyOf(input);
    assertEquals(tags.asMap(), map);
  }


  @Test
  public void testOfVarargTag() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    TagList tags = BasicTagList.of(
        new BasicTag("foo", "bar"), new BasicTag("dee", "dum"));
    assertEquals(tags.asMap(), map);
  }

  @Test
  public void testConcatVararg() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    TagList t1 = BasicTagList.of("foo", "bar");
    TagList tags = BasicTagList.concat(t1, new BasicTag("dee", "dum"));
    assertEquals(tags.asMap(), map);
  }

  @Test
  public void testConcatTagList() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    TagList t1 = BasicTagList.of("foo", "bar");
    TagList t2 = BasicTagList.of("dee", "dum");
    TagList tags = BasicTagList.concat(t1, t2);
    assertEquals(tags.asMap(), map);
  }

  @Test
  public void testConcatOverride() throws Exception {
    Map<String, String> map = mapOf("foo", "bar2");
    TagList t1 = BasicTagList.of("foo", "bar");
    TagList t2 = BasicTagList.of("foo", "bar2");
    TagList tags = BasicTagList.concat(t1, t2);
    assertEquals(tags.asMap(), map);
  }

  @Test
  public void testEmpty() throws Exception {
    TagList t1 = BasicTagList.EMPTY;
    assertTrue(t1.isEmpty());
    assertEquals(t1.size(), 0);
  }

  @Test
  public void testAccessors() throws Exception {
    TagList t1 = BasicTagList.of("foo", "bar");
    assertTrue(!t1.isEmpty());
    assertEquals(t1.size(), 1);
    assertEquals(t1.getTag("foo"), new BasicTag("foo", "bar"));
    assertTrue(t1.getTag("dee") == null, "dee is not a tag");
    assertTrue(t1.containsKey("foo"));
    assertTrue(!t1.containsKey("dee"));
  }

  @Test
  public void testIterator() throws Exception {
    TagList t1 = BasicTagList.of("foo", "bar");
    for (Tag t : t1) {
      assertEquals(t, new BasicTag("foo", "bar"));
    }
  }

  @Test
  public void testCopyTagList() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    BasicTagList t1 = BasicTagList.of("foo", "bar");
    BasicTagList t2 = BasicTagList.of("foo", "bar2");
    BasicTagList t3 = BasicTagList.of("dee", "dum");
    assertEquals(t1.copy(t2), t2);
    assertEquals(t1.copy(t3).asMap(), map);
  }

  @Test
  public void testCopy() throws Exception {
    Map<String, String> map = mapOf("foo", "bar", "dee", "dum");
    BasicTagList t1 = BasicTagList.of("foo", "bar");
    BasicTagList t2 = BasicTagList.of("foo", "bar2");
    assertEquals(t1.copy("foo", "bar2"), t2);
    assertEquals(t1.copy("dee", "dum").asMap(), map);
  }

  @Test
  public void testEquals() throws Exception {
    BasicTagList t1 = BasicTagList.of("foo", "bar");
    BasicTagList t2 = BasicTagList.of("foo", "bar2");
    BasicTagList t3 = BasicTagList.of("foo", "bar");

    assertNotNull(t1);
    assertFalse(t1.toString().equals(t2.toString()));
    assertTrue(t1.equals(t1));
    assertFalse(t1.equals(t2));
    assertTrue(t1.equals(t3));
  }

  @Test
  public void testHashCode() throws Exception {
    BasicTagList t1 = BasicTagList.of("foo", "bar");
    BasicTagList t2 = BasicTagList.of("foo", "bar2");
    BasicTagList t3 = BasicTagList.of("foo", "bar");

    assertTrue(t1.hashCode() == t1.hashCode());
    assertTrue(t1.hashCode() != t2.hashCode());
    assertTrue(t1.hashCode() == t3.hashCode());
  }

  @Test
  public void testOf() throws Exception {
    BasicTagList expected = BasicTagList.copyOf(mapOf("foo", "bar", "id", "1"));
    BasicTagList of = BasicTagList.of("foo", "bar", "id", "1");
    assertEquals(of, expected);
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testOfOddNumber() {
    BasicTagList.of("foo");
  }

  @Test
  public void testConcurrentTagList() throws Exception {
    final int count = 10;
    final CountDownLatch latch = new CountDownLatch(count);
    final Set<BasicTagList> tagLists = Collections
        .newSetFromMap(new ConcurrentHashMap<>());

    final CyclicBarrier barrier = new CyclicBarrier(count);

    for (int i = 0; i < count; i++) {
      new Thread(() -> {
        try {
          barrier.await();
          tagLists.add(BasicTagList.of("id", "1", "color",
              "green"));
        } catch (Exception e) {
          e.printStackTrace(System.out);
        } finally {
          latch.countDown();
        }
      }).start();
    }
    latch.await();
    assertEquals(tagLists.size(), 1);
  }
}

