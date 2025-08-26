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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * First, instatiate with the streams. Then add a data consumer, or create a message control and pass this as the
 * transport (which will make the message control the data consumer). Change the read delay if desired. This class
 * supports running in its own thread (start) or an external one (run), say from a thread pool. Both approaches are
 * delegated to the stream listener. In either case, stop the transport with the stop method (or just stop the message
 * control).
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class StreamTransport implements Transport, Runnable {
    protected OutputStream out;
    protected InputStream in;
    private InputStreamListener listener;

    /**
     * <p>Constructor for StreamTransport.</p>
     *
     * @param in  a {@link InputStream} object.
     * @param out a {@link OutputStream} object.
     */
    public StreamTransport(InputStream in, OutputStream out) {
        this.out = out;
        this.in = in;
    }

    /**
     * <p>setReadDelay.</p>
     *
     * @param readDelay a int.
     */
    public void setReadDelay(int readDelay) {
        if (listener != null)
            listener.setReadDelay(readDelay);
    }

    /**
     * <p>start.</p>
     *
     * @param threadName a {@link String} object.
     */
    public void start(String threadName) {
        listener.start(threadName);
    }

    /**
     * <p>stop.</p>
     */
    public void stop() {
        listener.stop();
    }

    /**
     * <p>run.</p>
     */
    public void run() {
        listener.run();
    }


    public void setConsumer(DataConsumer consumer) {
        listener = new InputStreamListener(in, consumer);
    }

    /**
     * <p>removeConsumer.</p>
     */
    public void removeConsumer() {
        listener.stop();
        listener = null;
    }

    /**
     * <p>write.</p>
     *
     * @param data an array of {@link byte} objects.
     * @throws IOException if any.
     */
    public void write(byte[] data) throws IOException {
        out.write(data);
        out.flush();
    }


    public void write(byte[] data, int len) throws IOException {
        out.write(data, 0, len);
        out.flush();
    }
}
