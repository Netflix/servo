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
package com.netflix.servo.publish;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;
import com.netflix.servo.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

/**
 * Writes observations to a file. The format is a basic text file with tabs
 * separating the fields.
 */
public final class FileMetricObserver extends BaseMetricObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(FileMetricObserver.class);

    private static final String FILE_DATE_FORMAT = "yyyy_dd_MM_HH_mm_ss_SSS";
    private final File dir;
    private final boolean compress;
    private final SimpleDateFormat fileFormat;

    /**
     * Creates a new instance that stores files in {@code dir} with a prefix of
     * {@code name} and a suffix of a timestamp in the format
     * {@code yyyy_dd_MM_HH_mm_ss_SSS}.
     *
     * @param name      name to use as a prefix on files
     * @param dir       directory where observations will be stored
     */
    public FileMetricObserver(String name, File dir) {
        this(name, dir, false);
    }

    /**
     * Creates a new instance that stores files in {@code dir} with a prefix of
     * {@code name} and a suffix of a timestamp in the format
     * {@code yyyy_dd_MM_HH_mm_ss_SSS}.
     *
     * @param name      name to use as a prefix on files
     * @param dir       directory where observations will be stored
     * @param compress  whether to compress our output
     */
    public FileMetricObserver(String name, File dir, boolean compress) {
        this(name,
            String.format("'%s'_%s", name, FILE_DATE_FORMAT) + (compress ? "'.log.gz'" : "'.log'"),
            dir,
            compress);
    }

    /**
     * Creates a new instance that stores files in {@code dir} with a name that
     * is created using {@code namePattern}.
     *
     * @param name         name of the observer
     * @param namePattern  date format pattern used to create the file names
     * @param dir          directory where observations will be stored
     * @param compress     whether to compress our output
     */
    public FileMetricObserver(String name, String namePattern, File dir, boolean compress) {
        super(name);
        this.dir = dir;
        this.compress = compress;
        fileFormat = new SimpleDateFormat(namePattern);
        fileFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /** {@inheritDoc} */
    public void updateImpl(List<Metric> metrics) {
        Preconditions.checkNotNull(metrics);
        File file = new File(dir, fileFormat.format(new Date()));
        Closer closer = Closer.create();
        Writer out = null;
        try {
            try {
                LOGGER.debug("writing {} metrics to file {}", metrics.size(), file);
                OutputStream fileOut = new FileOutputStream(file, true);
                if (compress) {
                    fileOut = new GZIPOutputStream(fileOut);
                }
                out = closer.register(new OutputStreamWriter(fileOut, "UTF-8"));
                for (Metric m : metrics) {
                    out.append(m.getConfig().getName()).append('\t')
                       .append(m.getConfig().getTags().toString()).append('\t')
                       .append(m.getValue().toString()).append('\n');
                }
            } catch (Throwable t) {
                throw closer.rethrow(t);
            } finally {
                closer.close();
            }
        } catch (IOException e) {
            incrementFailedCount();
            LOGGER.error("failed to write update to file " + file, e);
        }
    }
}
