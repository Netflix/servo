/*
 * #%L
 * servo
 * %%
 * Copyright (C) 2011 Netflix
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.netflix.servo.publish;

import com.google.common.base.Preconditions;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes observations to a file. The format is a basic text file with tabs
 * separating the fields.
 */
public final class FileMetricObserver implements MetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FileMetricObserver.class);

    private final File mFile;

    public FileMetricObserver(File file) {
        mFile = file;
    }

    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        Writer out = null;
        try {
            out = new FileWriter(mFile, true);
            for (Metric m : metrics) {
                out.append(m.name()).append('\t')
                   .append(m.tags().toString()).append('\t')
                   .append(Long.toString(m.timestamp())).append('\t')
                   .append(m.value().toString()).append('\n');
            }
        } catch (IOException e) {
            LOGGER.error("failed to write update to file " + mFile, e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.warn("close failed for file " + mFile, e);
                }
            }
        }
    }
}
