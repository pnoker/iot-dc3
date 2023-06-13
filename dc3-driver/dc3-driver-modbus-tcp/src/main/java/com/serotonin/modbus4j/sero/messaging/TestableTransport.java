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

package com.serotonin.modbus4j.sero.messaging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides synchronization on the input stream read by wrapping it.
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
