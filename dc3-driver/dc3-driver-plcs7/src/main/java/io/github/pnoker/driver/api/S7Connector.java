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
package io.github.pnoker.driver.api;

import java.io.Closeable;

/**
 * Interface for connecting to Siemens S7 PLCs. This interface provides methods to read
 * from and write to different memory areas in Siemens S7 programmable logic controllers.
 *
 * @author Thomas Rudin
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface S7Connector extends Closeable {

    /**
     * Read bytes from a PLC memory area.
     *
     * @param area       the memory area type
     * @param areaNumber area number (e.g. DB number)
     * @param bytes      number of bytes to read
     * @param offset     byte offset within the area
     * @return the bytes read from the PLC
     */
    byte[] read(DaveArea area, int areaNumber, int bytes, int offset);

    /**
     * Write bytes to a PLC memory area.
     *
     * @param area       the memory area type
     * @param areaNumber area number (e.g. DB number)
     * @param offset     byte offset within the area
     * @param buffer     the byte array to write
     */
    void write(DaveArea area, int areaNumber, int offset, byte[] buffer);

}
