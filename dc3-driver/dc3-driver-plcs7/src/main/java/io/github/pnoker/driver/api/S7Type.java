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

import io.github.pnoker.driver.api.impl.serializer.converter.*;

/**
 * Type of the Address
 *
 * @author Thomas Rudin Libnodave:
 * <a href="http://libnodave.sourceforge.net/">libnodave.sourceforge.net</a>
 */
public enum S7Type {
    /**
     * Boolean type
     */
    BOOL(BitConverter.class, 0, 1),

    /**
     * Byte type
     */
    BYTE(ByteConverter.class, 1, 0),

    /**
     * A INT-type
     */
    INT(ShortConverter.class, 2, 0),

    /**
     * A DINT-type (same as DWORD-type)
     */
    DINT(LongConverter.class, 4, 0),

    /**
     * A Word-type (same as int-type)
     */
    WORD(IntegerConverter.class, 2, 0),
    /**
     * Double word
     */
    DWORD(LongConverter.class, 4, 0),

    /**
     * Real-type, corresponds to float or double
     */
    REAL(RealConverter.class, 4, 0),

    /**
     * String type, size must be specified manually
     */
    STRING(StringConverter.class, 2, 0),

    /**
     * Simple Date with 2 bytes in length
     */
    DATE(DateConverter.class, 2, 0),

    /**
     * Time-type, 4 bytes in length, number of millis
     */
    TIME(TimeConverter.class, 4, 0),

    /**
     * Full Date and time format with precision in milliseconds
     */
    DATE_AND_TIME(DateAndTimeConverter.class, 8, 0),

    /**
     * Structure type
     */
    STRUCT(StructConverter.class, 0, 0);

    private final int byteSize;
    private final int bitSize;

    private final Class<? extends S7Serializable> serializer;

    /**
     * Enum Constructor
     *
     * @param serializer S7Serializable
     * @param byteSize   A Byte Size
     * @param bitSize    A bit Size
     */
    S7Type(final Class<? extends S7Serializable> serializer, final int byteSize, final int bitSize) {
        this.serializer = serializer;
        this.bitSize = bitSize;
        this.byteSize = byteSize;
    }

    public int getBitSize() {
        return this.bitSize;
    }

    public int getByteSize() {
        return this.byteSize;
    }

    public Class<? extends S7Serializable> getSerializer() {
        return this.serializer;
    }
}
