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

import com.serotonin.modbus4j.sero.io.NullWriter;
import com.serotonin.modbus4j.sero.io.StreamUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>Abstract BaseIOLog class.</p>
 *
 * @author Terry Packer
 * @version 2025.6.0
 */
public abstract class BaseIOLog {

    /**
     * Constant <code>DATE_FORMAT="yyyy/MM/dd-HH:mm:ss,SSS"</code>
     */
    protected static final String DATE_FORMAT = "yyyy/MM/dd-HH:mm:ss,SSS";
    private static final Log LOG = LogFactory.getLog(BaseIOLog.class);
    protected final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    protected final File file;
    protected final StringBuilder sb = new StringBuilder();
    protected final Date date = new Date();
    protected PrintWriter out;

    /**
     * <p>Constructor for BaseIOLog.</p>
     *
     * @param logFile a {@link File} object.
     */
    public BaseIOLog(File logFile) {
        this.file = logFile;
        createOut();
    }

    /**
     * Create the Print Writer output
     */
    protected void createOut() {
        try {
            out = new PrintWriter(new FileWriter(file, true));
        } catch (IOException e) {
            out = new PrintWriter(new NullWriter());
            LOG.error("Error while creating process log", e);
        }
    }

    /**
     * <p>close.</p>
     */
    public void close() {
        out.close();
    }

    /**
     * <p>input.</p>
     *
     * @param b an array of {@link byte} objects.
     */
    public void input(byte[] b) {
        log(true, b, 0, b.length);
    }

    /**
     * <p>input.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     */
    public void input(byte[] b, int pos, int len) {
        log(true, b, pos, len);
    }

    /**
     * <p>output.</p>
     *
     * @param b an array of {@link byte} objects.
     */
    public void output(byte[] b) {
        log(false, b, 0, b.length);
    }

    /**
     * <p>output.</p>
     *
     * @param b   an array of {@link byte} objects.
     * @param pos a int.
     * @param len a int.
     */
    public void output(byte[] b, int pos, int len) {
        log(false, b, pos, len);
    }

    /**
     * <p>log.</p>
     *
     * @param input a boolean.
     * @param b     an array of {@link byte} objects.
     */
    public void log(boolean input, byte[] b) {
        log(input, b, 0, b.length);
    }

    /**
     * <p>log.</p>
     *
     * @param input a boolean.
     * @param b     an array of {@link byte} objects.
     * @param pos   a int.
     * @param len   a int.
     */
    public synchronized void log(boolean input, byte[] b, int pos, int len) {
        sizeCheck();

        sb.delete(0, sb.length());
        date.setTime(System.currentTimeMillis());
        sb.append(sdf.format(date)).append(" ");
        sb.append(input ? "I" : "O").append(" ");
        sb.append(StreamUtils.dumpHex(b, pos, len));
        out.println(sb.toString());
        out.flush();
    }

    /**
     * <p>log.</p>
     *
     * @param message a {@link String} object.
     */
    public synchronized void log(String message) {
        sizeCheck();

        sb.delete(0, sb.length());
        date.setTime(System.currentTimeMillis());
        sb.append(sdf.format(date)).append(" ");
        sb.append(message);
        out.println(sb.toString());
        out.flush();
    }

    /**
     * Check the size of the logfile and perform adjustments
     * as necessary
     */
    protected abstract void sizeCheck();
}
