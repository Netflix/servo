/**
 * Copyright 2017 Netflix, Inc.
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;

import java.io.IOException;

/**
 * A simple Jackson Pretty Printer that helps dump atlas payloads in a format that is easy to grep.
 */
class AtlasPrettyPrinter implements PrettyPrinter {
  private int nesting = 0;

  @Override
  public void writeRootValueSeparator(JsonGenerator jg) throws IOException {
  }

  @Override
  public void writeStartObject(JsonGenerator gen) throws IOException {
    gen.writeRaw('{');
    if (nesting == 0) {
      gen.writeRaw('\n');
    }
    ++nesting;
  }

  @Override
  public void writeEndObject(JsonGenerator gen, int nrOfEntries) throws IOException {
    --nesting;
    gen.writeRaw('}');
  }

  @Override
  public void writeObjectEntrySeparator(JsonGenerator gen) throws IOException {
    gen.writeRaw(',');
    if (nesting <= 1) {
      gen.writeRaw('\n');
    }
  }

  @Override
  public void writeObjectFieldValueSeparator(JsonGenerator gen) throws IOException {
    gen.writeRaw(':');
  }

  @Override
  public void writeStartArray(JsonGenerator gen) throws IOException {
    gen.writeRaw("[\n");
  }

  @Override
  public void writeEndArray(JsonGenerator gen, int nrOfValues) throws IOException {
    gen.writeRaw("]\n");
  }

  @Override
  public void writeArrayValueSeparator(JsonGenerator gen) throws IOException {
    gen.writeRaw(',');
    if (nesting == 1) {
      gen.writeRaw('\n');
    }
  }

  @Override
  public void beforeArrayValues(JsonGenerator gen) throws IOException {
  }

  @Override
  public void beforeObjectEntries(JsonGenerator gen) throws IOException {
  }
}
