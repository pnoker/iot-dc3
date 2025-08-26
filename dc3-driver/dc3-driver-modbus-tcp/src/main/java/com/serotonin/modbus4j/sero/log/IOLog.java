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

import java.io.File;

/**
 * <p>IOLog class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class IOLog extends BaseIOLog {
    //private static final Log LOG = LogFactory.getLog(IOLog.class);
    private static final int MAX_FILESIZE = 1000000;
    //    private static final int MAX_FILESIZE = 1000;
    private final File backupFile;

    /**
     * <p>Constructor for IOLog.</p>
     *
     * @param filename a {@link String} object.
     */
    public IOLog(String filename) {
        super(new File(filename));
        backupFile = new File(filename + ".1");
    }


    @Override
    protected void sizeCheck() {
        // Check if the file should be rolled.
        if (file.length() > MAX_FILESIZE) {
            out.close();

            if (backupFile.exists())
                backupFile.delete();
            file.renameTo(backupFile);
            createOut();
        }
    }
    //
    //    public static void main(String[] args) {
    //        byte[] b = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
    //
    //        IOLog log = new IOLog("iotest");
    //        log.log("test");
    //        log.log("testtest");
    //
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //        log.input(b);
    //        log.output(b);
    //
    //        log.log("testtesttesttesttesttesttesttesttesttest");
    //    }
}
