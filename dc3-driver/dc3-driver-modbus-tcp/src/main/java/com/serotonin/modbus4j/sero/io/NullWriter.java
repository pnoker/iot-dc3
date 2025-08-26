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

package com.serotonin.modbus4j.sero.io;

import java.io.IOException;
import java.io.Writer;

/**
 * <p>NullWriter class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class NullWriter extends Writer {
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // no op
    }

    @Override
    public void flush() throws IOException {
        // no op
    }

    @Override
    public void close() throws IOException {
        // no op
    }
}
