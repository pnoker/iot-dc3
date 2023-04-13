/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.serotonin.modbus4j.sero.log;

import java.io.File;

/**
 * <p>IOLog class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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


    /**
     * {@inheritDoc}
     */
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
