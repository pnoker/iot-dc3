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
import com.serotonin.modbus4j.sero.epoll.Modbus4JInputStreamCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * First, instatiate with the streams and epoll. Then add a data consumer, or create a message control and pass this as
 * the transport (which will make the message control the data consumer). Stop the transport by stopping the message
 * control).
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class EpollStreamTransport implements Transport {
    private final OutputStream out;
    private final InputStream in;
    private final InputStreamEPollWrapper epoll;

    /**
     * <p>Constructor for EpollStreamTransport.</p>
     *
     * @param in    a {@link InputStream} object.
     * @param out   a {@link OutputStream} object.
     * @param epoll a {@link InputStreamEPollWrapper} object.
     */
    public EpollStreamTransport(InputStream in, OutputStream out, InputStreamEPollWrapper epoll) {
        this.out = out;
        this.in = in;
        this.epoll = epoll;
    }

    @Override
    public void setConsumer(final DataConsumer consumer) {
        epoll.add(in, new Modbus4JInputStreamCallback() {
            @Override
            public void terminated() {
                removeConsumer();
            }

            @Override
            public void ioException(IOException e) {
                consumer.handleIOException(e);
            }

            @Override
            public void input(byte[] buf, int len) {
                consumer.data(buf, len);
            }

            @Override
            public void closed() {
                removeConsumer();
            }
        });
    }

    /**
     * <p>removeConsumer.</p>
     */
    @Override
    public void removeConsumer() {
        epoll.remove(in);
    }

    /**
     * <p>write.</p>
     *
     * @param data an array of {@link byte} objects.
     * @throws IOException if any.
     */
    @Override
    public void write(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }

    @Override
    public void write(byte[] data, int len) throws IOException {
        out.write(data, 0, len);
        out.flush();
    }
}
