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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>StreamTransportCharSpaced class.</p>
 *
 * @author Terry Packer
 * @version 5.0.0
 */
public class StreamTransportCharSpaced extends StreamTransport {

    private final long charSpacing;

    /**
     * <p>Constructor for StreamTransportCharSpaced.</p>
     *
     * @param in          a {@link InputStream} object.
     * @param out         a {@link OutputStream} object.
     * @param charSpacing a long.
     */
    public StreamTransportCharSpaced(InputStream in, OutputStream out, long charSpacing) {
        super(in, out);
        this.charSpacing = charSpacing;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Perform a write, ensure space between chars
     */
    @Override
    public void write(byte[] data) throws IOException {

        try {
            long waited = 0, writeStart, writeEnd, waitRemaining;
            for (byte b : data) {
                writeStart = System.nanoTime();
                out.write(b);
                writeEnd = System.nanoTime();
                waited = writeEnd - writeStart;
                if (waited < this.charSpacing) {
                    waitRemaining = this.charSpacing - waited;
                    Thread.sleep(waitRemaining / 1000000, (int) (waitRemaining % 1000000));
                }

            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        out.flush();
    }

    /**
     * {@inheritDoc}
     */
    public void write(byte[] data, int len) throws IOException {
        try {
            long waited = 0, writeStart, writeEnd, waitRemaining;
            for (int i = 0; i < len; i++) {
                writeStart = System.nanoTime();
                out.write(data[i]);
                writeEnd = System.nanoTime();
                waited = writeEnd - writeStart;
                if (waited < this.charSpacing) {
                    waitRemaining = this.charSpacing - waited;
                    Thread.sleep(waitRemaining / 1000000, (int) (waitRemaining % 1000000));
                }

            }
        } catch (Exception e) {
            throw new IOException(e);
        }
        out.flush();
    }


}
