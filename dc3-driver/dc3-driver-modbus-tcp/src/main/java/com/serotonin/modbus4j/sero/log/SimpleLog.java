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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>SimpleLog class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class SimpleLog {
    private final PrintWriter out;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");
    private final StringBuilder sb = new StringBuilder();
    private final Date date = new Date();

    /**
     * <p>Constructor for SimpleLog.</p>
     */
    public SimpleLog() {
        this(new PrintWriter(System.out));
    }

    /**
     * <p>Constructor for SimpleLog.</p>
     *
     * @param out a {@link PrintWriter} object.
     */
    public SimpleLog(PrintWriter out) {
        this.out = out;
    }

    /**
     * <p>out.</p>
     *
     * @param message a {@link String} object.
     */
    public void out(String message) {
        out(message, null);
    }

    /**
     * <p>out.</p>
     *
     * @param t a {@link Throwable} object.
     */
    public void out(Throwable t) {
        out(null, t);
    }

    /**
     * <p>out.</p>
     *
     * @param o a {@link Object} object.
     */
    public void out(Object o) {
        if (o instanceof Throwable)
            out(null, (Throwable) o);
        else if (o == null)
            out(null, null);
        else
            out(o.toString(), null);
    }

    /**
     * <p>close.</p>
     */
    public void close() {
        out.close();
    }

    /**
     * <p>out.</p>
     *
     * @param message a {@link String} object.
     * @param t       a {@link Throwable} object.
     */
    public synchronized void out(String message, Throwable t) {
        sb.delete(0, sb.length());
        date.setTime(System.currentTimeMillis());
        sb.append(sdf.format(date)).append(" ");
        if (message != null)
            sb.append(message);
        if (t != null) {
            if (t.getMessage() != null)
                sb.append(" - ").append(t.getMessage());
            out.println(sb.toString());
            t.printStackTrace(out);
        } else
            out.println(sb.toString());
        out.flush();
    }
}
