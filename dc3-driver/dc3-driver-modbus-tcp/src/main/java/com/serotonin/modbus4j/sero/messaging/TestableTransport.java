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

package com.serotonin.modbus4j.sero.messaging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides synchronization on the input stream read by wrapping it.
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class TestableTransport extends StreamTransport {
    /**
     * <p>Constructor for TestableTransport.</p>
     *
     * @param in  a {@link InputStream} object.
     * @param out a {@link OutputStream} object.
     */
    public TestableTransport(InputStream in, OutputStream out) {
        super(new TestableBufferedInputStream(in), out);
    }

    /**
     * <p>testInputStream.</p>
     *
     * @throws IOException if any.
     */
    public void testInputStream() throws IOException {
        ((TestableBufferedInputStream) in).test();
    }

    static class TestableBufferedInputStream extends BufferedInputStream {
        public TestableBufferedInputStream(InputStream in) {
            super(in);
        }

        @Override
        public synchronized int read(byte[] buf) throws IOException {
            return super.read(buf);
        }

        public synchronized void test() throws IOException {
            mark(1);
            int i = read();
            if (i == -1)
                throw new IOException("Stream closed");
            reset();
        }
    }
}
