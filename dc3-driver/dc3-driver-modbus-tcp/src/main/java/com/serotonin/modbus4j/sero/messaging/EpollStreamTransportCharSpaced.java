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

import com.serotonin.modbus4j.sero.epoll.InputStreamEPollWrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>EpollStreamTransportCharSpaced class.</p>
 *
 * @author Terry Packer
 * @version 2025.6.0
 */
public class EpollStreamTransportCharSpaced extends EpollStreamTransport {

    private final long charSpacing; //Spacing for chars in nanoseconds
    private final OutputStream out; //Since the subclass has private members

    /**
     * <p>Constructor for EpollStreamTransportCharSpaced.</p>
     *
     * @param in          a {@link InputStream} object.
     * @param out         a {@link OutputStream} object.
     * @param epoll       a {@link InputStreamEPollWrapper} object.
     * @param charSpacing a long.
     */
    public EpollStreamTransportCharSpaced(InputStream in, OutputStream out,
                                          InputStreamEPollWrapper epoll, long charSpacing) {
        super(in, out, epoll);
        this.out = out;
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
