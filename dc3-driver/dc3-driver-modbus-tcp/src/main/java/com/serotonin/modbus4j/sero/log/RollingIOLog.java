/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.serotonin.modbus4j.sero.log;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Class to Log IO with the option to keep historical files
 *
 * @author Terry Packer
 * @version 2025.6.0
 */
public class RollingIOLog extends BaseIOLog {

    private static final Log LOG = LogFactory.getLog(RollingIOLog.class);

    //New Members
    protected int fileSize;
    protected int maxFiles;
    protected int currentFileNumber;

    /**
     * <p>Constructor for RollingIOLog.</p>
     *
     * @param baseFilename - The base filename for all logfiles ie. dataLog.log
     * @param logDirectory a {@link File} object.
     * @param fileSize     - in bytes of file before rolling over
     * @param maxFiles     - max number to keep in addition to the current log file
     */
    public RollingIOLog(final String baseFilename, File logDirectory, int fileSize, int maxFiles) {
        super(new File(logDirectory, baseFilename));  //Ignoring this
        createOut();

        //Detect the current file number
        File[] files = logDirectory.listFiles(new LogFilenameFilter(baseFilename));

        //files will contain baseFilename.log, baseFilename.log.1 ... baseFilename.log.n
        // where n is our currentFileNumber
        this.currentFileNumber = files.length - 1;
        if (this.currentFileNumber > maxFiles)
            this.currentFileNumber = maxFiles;

        this.fileSize = fileSize;
        this.maxFiles = maxFiles;

    }


    @Override
    protected void sizeCheck() {
        // Check if the file should be rolled.
        if (file.length() > this.fileSize) {
            out.close();

            try {
                //Do rollover

                for (int i = this.currentFileNumber; i > 0; i--) {
                    Path source = Paths.get(this.file.getAbsolutePath() + "." + i);
                    Path target = Paths.get(this.file.getAbsolutePath() + "." + (i + 1));
                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                }

                Path source = Paths.get(this.file.toURI());
                Path target = Paths.get(this.file.getAbsolutePath() + "." + 1);
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                if (this.currentFileNumber < this.maxFiles - 1) {
                    //Use file number
                    this.currentFileNumber++;
                }

            } catch (IOException e) {
                LOG.error(e);
            }

            createOut();
        }
    }


    /**
     * Class to filter log filenames from a directory listing
     *
     * @author Terry Packer
     */
    class LogFilenameFilter implements FilenameFilter {

        private String nameToMatch;

        public LogFilenameFilter(String nameToMatch) {
            this.nameToMatch = nameToMatch;
        }

        @Override
        public boolean accept(File dir, String name) {
            return name.contains(this.nameToMatch);
        }

    }

}
