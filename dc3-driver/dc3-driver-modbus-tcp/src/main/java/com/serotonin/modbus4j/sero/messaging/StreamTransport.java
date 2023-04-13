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
 * First, instatiate with the streams. Then add a data consumer, or create a message control and pass this as the
 * transport (which will make the message control the data consumer). Change the read delay if desired. This class
 * supports running in its own thread (start) or an external one (run), say from a thread pool. Both approaches are
 * delegated to the stream listener. In either case, stop the transport with the stop method (or just stop the message
 * control).
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    public void write(byte[] data, int len) throws IOException {
        out.write(data, 0, len);
        out.flush();
    }
}
