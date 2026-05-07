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
 * S7 PLC data types with their associated serializers and sizes.
 *
 * @author Thomas Rudin Libnodave:
 * <a href="http://libnodave.sourceforge.net/">libnodave.sourceforge.net</a>
 */
public enum S7Type {

    BOOL(BitConverter.class, 0, 1), BYTE(ByteConverter.class, 1, 0), INT(ShortConverter.class, 2, 0),
    DINT(LongConverter.class, 4, 0), WORD(IntegerConverter.class, 2, 0), DWORD(LongConverter.class, 4, 0),
    REAL(RealConverter.class, 4, 0),
    /**
     * String type; size must be specified manually.
     */
    STRING(StringConverter.class, 2, 0), DATE(DateConverter.class, 2, 0),
    /**
     * Duration in milliseconds, 4 bytes.
     */
    TIME(TimeConverter.class, 4, 0),
    /**
     * Date and time with millisecond precision, 8 bytes.
     */
    DATE_AND_TIME(DateAndTimeConverter.class, 8, 0), STRUCT(StructConverter.class, 0, 0);

    private final int byteSize;

    private final int bitSize;

    private final Class<? extends S7Serializable> serializer;

    S7Type(final Class<? extends S7Serializable> serializer, final int byteSize, final int bitSize) {
        this.serializer = serializer;
        this.bitSize = bitSize;
        this.byteSize = byteSize;
    }

    /**
     * @return size of this type in bits
     */
    public int getBitSize() {
        return this.bitSize;
    }

    /**
     * @return size of this type in bytes
     */
    public int getByteSize() {
        return this.byteSize;
    }

    /**
     * @return the serializer class for this type
     */
    public Class<? extends S7Serializable> getSerializer() {
        return this.serializer;
    }

}
