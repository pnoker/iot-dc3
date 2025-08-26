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

/**
 * A transport is a wrapper around the means by which data is transferred. So, there could be transports for serial
 * ports, sockets, UDP, email, etc.
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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
