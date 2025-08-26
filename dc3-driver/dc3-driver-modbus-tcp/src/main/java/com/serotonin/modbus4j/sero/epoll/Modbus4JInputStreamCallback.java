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

package com.serotonin.modbus4j.sero.epoll;

import java.io.IOException;

/**
 * A callback interface for input streams.
 * <p>
 * NOTE: if the InputStreamEPoll instance is terminated, any running processes will be destroyed without any
 * notification to this callback.
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public interface Modbus4JInputStreamCallback {
    /**
     * Called when content is read from the input stream.
     *
     * @param buf the content that was read. This is a shared byte array. Contents can be manipulated within this call,
     *            but the array itself should not be stored beyond the call since the contents will be changed.
     * @param len the length of content that was read.
     */
    void input(byte[] buf, int len);

    /**
     * Called when the closure of the input stream is detected.
     */
    void closed();

    /**
     * Called if there is an {@link IOException} while reading input stream.
     *
     * @param e the exception that was received
     */
    void ioException(IOException e);

    /**
     * Called if the InputStreamEPoll instance was terminated while the input stream was still registered.
     */
    void terminated();
}
