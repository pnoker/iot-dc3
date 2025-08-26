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

import java.io.InputStream;

/**
 * <p>InputStreamEPollWrapper interface.</p>
 *
 * @author Terry Packer
 * @version 2025.6.0
 */
public interface InputStreamEPollWrapper {

    /**
     * <p>add.</p>
     *
     * @param in                  a {@link InputStream} object.
     * @param inputStreamCallback a {@link Modbus4JInputStreamCallback} object.
     */
    void add(InputStream in, Modbus4JInputStreamCallback inputStreamCallback);

    /**
     * <p>remove.</p>
     *
     * @param in a {@link InputStream} object.
     */
    void remove(InputStream in);

}
