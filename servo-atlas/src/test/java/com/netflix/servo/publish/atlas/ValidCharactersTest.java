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

import org.testng.annotations.Test;

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
    String str = "Aabc09.-_ abc";
    assertEquals(ValidCharacters.toValidCharset(str), "Aabc09.-__abc");

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
    String str = "abc09.-_ abc";
    assertTrue(ValidCharacters.hasInvalidCharacters(str));

    String boundaries = "\u0000\u0128\uffff";
    assertTrue(ValidCharacters.hasInvalidCharacters(boundaries));
  }
}