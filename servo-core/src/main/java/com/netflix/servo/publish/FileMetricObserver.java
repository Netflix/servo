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
import com.google.common.io.Closeables;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes observations to a file. The format is a basic text file with tabs
 * separating the fields.
 */
public final class FileMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FileMetricObserver.class);

    private static final String FILE_DATE_FORMAT = "yyyy_dd_MM_HH_mm_ss_SSS";
    private static final String ISO_DATE_FORMAT = "yyyy-dd-MM'T'HH:mm:ss.SSS";

    private final File dir;

    private final SimpleDateFormat fileFormat;
    private final SimpleDateFormat isoFormat;

    /**
     * Creates a new instance that stores files in {@code dir} with a prefix of
     * {@code name} and a suffix of a timestamp in the format
     * {@code yyyy_dd_MM_HH_mm_ss_SSS}. 
     *
     * @param name  name to use as a prefix on files
     * @param dir   directory where observations will be stored
     */
    public FileMetricObserver(String name, File dir) {
        this(name, String.format("'%s'_%s'.log'", name, FILE_DATE_FORMAT), dir);
    }

    /**
     * Creates a new instance that stores files in {@code dir} with a name that
     * is created using {@code namePattern}.
     *
     * @param name         name of the observer
     * @param namePattern  date format pattern used to create the file names
     * @param dir          directory where observations will be stored
     */
    public FileMetricObserver(String name, String namePattern, File dir) {
        super(name);
        this.dir = dir;
        fileFormat = new SimpleDateFormat(namePattern);
        fileFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        isoFormat = new SimpleDateFormat(ISO_DATE_FORMAT);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** {@inheritDoc} */
    public void update(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        File file = new File(dir, fileFormat.format(new Date()));
        Writer out = null;
        try {
            LOGGER.debug("writing %d metrics to file %s", metrics.size(), file);
            out = new FileWriter(file, true);
            for (Metric m : metrics) {
                String timestamp = isoFormat.format(new Date(m.timestamp()));
                out.append(m.name()).append('\t')
                   .append(m.tags().toString()).append('\t')
                   .append(timestamp).append('\t')
                   .append(m.value().toString()).append('\n');
            }
        } catch (IOException e) {
            LOGGER.error("failed to write update to file " + file, e);
        } finally {
            Closeables.closeQuietly(out);
        }
    }
}
