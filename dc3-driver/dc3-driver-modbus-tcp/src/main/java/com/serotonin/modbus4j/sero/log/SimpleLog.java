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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>SimpleLog class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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
