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
package io.github.pnoker.driver.api.impl.serializer.converter;

import io.github.pnoker.driver.api.S7Serializable;
import io.github.pnoker.driver.api.S7Type;

public final class LongConverter implements S7Serializable {

    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final byte b1 = buffer[byteOffset];
        final byte b2 = buffer[byteOffset + 1];
        final byte b3 = buffer[byteOffset + 2];
        final byte b4 = buffer[byteOffset + 3];

        final Integer i = ((b4) & 0x000000FF) |
                ((b3 << 8) & 0x0000FF00) |
                ((b2 << 16) & 0x00FF0000) |
                ((b1 << 24) & 0xFF000000);

        return targetClass.cast(i.longValue());
    }

    @Override
    public S7Type getS7Type() {
        return S7Type.DWORD;
    }

    @Override
    public int getSizeInBits() {
        return 0;
    }

    @Override
    public int getSizeInBytes() {
        return 4;
    }

    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final Long value = (Long) javaType;
        final byte b4 = (byte) ((value) & 0xFF);
        final byte b3 = (byte) ((value >> 8) & 0xFF);
        final byte b2 = (byte) ((value >> 16) & 0xFF);
        final byte b1 = (byte) ((value >> 24) & 0xFF);
        buffer[byteOffset] = b1;
        buffer[byteOffset + 1] = b2;
        buffer[byteOffset + 2] = b3;
        buffer[byteOffset + 3] = b4;
    }

}
