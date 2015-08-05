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
package com.netflix.servo.publish;

import com.netflix.servo.Metric;
import com.netflix.servo.tag.SortedTagList;
import com.netflix.servo.tag.Tag;
import com.netflix.servo.tag.TagList;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class FileMetricObserverTest {

  private static final TagList TAGS = SortedTagList.builder()
      .withTag("cluster", "foo")
      .withTag("zone", "a")
      .withTag("node", "i-123")
      .build();

  private List<Metric> mkList(int v) {
    List<Metric> metrics = new ArrayList<>(v);
    for (int i = 0; i < v; ++i) {
      metrics.add(new Metric("m", TAGS, 0L, i));
    }
    return metrics;
  }

  private void delete(File f) throws IOException {
    if (f.exists() && !f.delete()) {
      throw new IOException("could not delete " + f);
    }
  }

  private void deleteRecursively(File f) throws IOException {
    if (f.isDirectory()) {
      for (File file : f.listFiles()) {
        deleteRecursively(file);
      }
      delete(f);
    } else {
      delete(f);
    }
  }

  private void checkLine(int i, String line) {
    String[] parts = line.split("\t");
    assertEquals(parts.length, 3);
    assertEquals(parts[0], "m");
    for (Tag tag : TAGS) {
      String tagStr = tag.getKey() + "=" + tag.getValue();
      assertTrue(parts[1].contains(tagStr), "missing " + tagStr);
    }
    assertEquals(Integer.parseInt(parts[2]), i);
  }

  private void checkFile(File f, boolean compressed) throws IOException {
    InputStream is = new FileInputStream(f);
    if (compressed) {
      is = new GZIPInputStream(is);
    }
    try (BufferedReader in = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
      int i = 0;
      String line;
      while ((line = in.readLine()) != null) {
        checkLine(i, line);
        ++i;
      }
    }
  }

  private static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < 3; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }
    throw new IllegalStateException("Failed to create directory within 3 attempts (tried "
        + baseName + "0 to " + baseName + "2)");
  }

  @Test
  public void testUpdate() throws Exception {
    File dir = createTempDir();
    try {
      MetricObserver fmo = new FileMetricObserver("test", dir);
      fmo.update(mkList(1));
      Thread.sleep(250);
      fmo.update(mkList(2));
      Thread.sleep(250);
      fmo.update(mkList(3));

      File[] files = dir.listFiles();
      assert files != null;
      assertEquals(files.length, 3);
      for (File f : files) {
        checkFile(f, false);
      }
    } finally {
      deleteRecursively(dir);
    }
  }

  @Test
  public void testUpdateCompressed() throws Exception {
    File dir = createTempDir();
    try {
      MetricObserver fmo = new FileMetricObserver("test", dir, true);
      fmo.update(mkList(1));
      Thread.sleep(250);
      fmo.update(mkList(2));
      Thread.sleep(250);
      fmo.update(mkList(3));

      File[] files = dir.listFiles();
      assert files != null;
      assertEquals(files.length, 3);
      for (File f : files) {
        checkFile(f, true);
      }
    } finally {
      deleteRecursively(dir);
    }
  }
}
