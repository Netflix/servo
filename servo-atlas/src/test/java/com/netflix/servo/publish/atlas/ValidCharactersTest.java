/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.servo.publish.atlas;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.netflix.servo.Metric;
import com.netflix.servo.monitor.MonitorConfig;
import com.netflix.servo.tag.BasicTag;
import com.netflix.servo.tag.BasicTagList;
import com.netflix.servo.tag.Tag;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

public class ValidCharactersTest {
  @Test
  public void testValidStrIsUnchanged() throws Exception {
    String valid = "abc09.-_";
    assertEquals(ValidCharacters.toValidCharset(valid), valid);
  }

  @Test
  public void testInvalidStrIsFixed() throws Exception {
    String str = "Aabc09.-~^_ abc";
    assertEquals(ValidCharacters.toValidCharset(str), "Aabc09.-____abc");

    String boundaries = "\u0000\u0128\uffff";
    assertEquals(ValidCharacters.toValidCharset(boundaries), "___");
  }

  @Test
  public void testValidStr() throws Exception {
    String valid = "AZabc09.-_";
    assertFalse(ValidCharacters.hasInvalidCharacters(valid));
  }

  @Test
  public void testInvalidStr() throws Exception {
    String caret = "abc09.-_^abc";
    assertTrue(ValidCharacters.hasInvalidCharacters(caret));

    String tilde = "abc09.-_~abc";
    assertTrue(ValidCharacters.hasInvalidCharacters(tilde));

    String str = "abc09.-_ abc";
    assertTrue(ValidCharacters.hasInvalidCharacters(str));

    String boundaries = "\u0000\u0128\uffff";
    assertTrue(ValidCharacters.hasInvalidCharacters(boundaries));
  }

  @Test
  public void testValidValue() throws Exception {
    MonitorConfig cfg = MonitorConfig.builder("foo^bar")
        .withTag("nf.asg", "foo~1")
        .withTag("nf.cluster", "foo^1.0")
        .withTag("key^1.0", "val~1.0")
        .build();
    Metric metric = new Metric(cfg, 0, 0.0);
    Metric fixed = ValidCharacters.toValidValue(metric);
    Metric expected = new Metric("foo_bar",
        BasicTagList.of("nf.asg", "foo~1", "nf.cluster", "foo^1.0", "key_1.0", "val_1.0"), 0, 0.0);
    assertEquals(fixed, expected);
  }

  private static JsonFactory factory = new JsonFactory();
  static {
    factory.enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
  }

  private static String toJson(Tag tag) throws IOException {
    StringWriter writer = new StringWriter();
    JsonGenerator generator = factory.createGenerator(writer);
    generator.writeStartObject();
    ValidCharacters.tagToJson(generator, tag);
    generator.writeEndObject();
    generator.close();
    return writer.toString();
  }

  @Test
  public void testTagToJson() throws Exception {
    Tag valid = new BasicTag("key", "value");
    assertEquals(toJson(valid), "{\"key\":\"value\"}");

    Tag invalidKey = new BasicTag("key~^a", "value");
    assertEquals(toJson(invalidKey), "{\"key__a\":\"value\"}");

    Tag invalidValue = new BasicTag("key", "value~^ 1");
    assertEquals(toJson(invalidValue), "{\"key\":\"value___1\"}");

    Tag relaxedValue = new BasicTag("nf.asg", "value~^ 1");
    assertEquals(toJson(relaxedValue), "{\"nf.asg\":\"value~^_1\"}");
  }
}
