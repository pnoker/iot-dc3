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

/**
 * A transport is a wrapper around the means by which data is transferred. So, there could be transports for serial
 * ports, sockets, UDP, email, etc.
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public interface Transport {
    /**
     * <p>setConsumer.</p>
     *
     * @param consumer a {@link DataConsumer} object.
     * @throws IOException if any.
     */
    abstract void setConsumer(DataConsumer consumer) throws IOException;

    /**
     * <p>removeConsumer.</p>
     */
    abstract void removeConsumer();

    /**
     * <p>write.</p>
     *
     * @param data an array of {@link byte} objects.
     * @throws IOException if any.
     */
    abstract void write(byte[] data) throws IOException;

    /**
     * <p>write.</p>
     *
     * @param data an array of {@link byte} objects.
     * @param len  a int.
     * @throws IOException if any.
     */
    abstract void write(byte[] data, int len) throws IOException;
}
