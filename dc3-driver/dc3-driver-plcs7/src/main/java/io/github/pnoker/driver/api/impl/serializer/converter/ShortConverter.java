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

public class ShortConverter implements S7Serializable {

    private static final short OFFSET_HIGH_BYTE = 0;
    private static final short OFFSET_LOW_BYTE = 1;

    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final byte lower = buffer[byteOffset + OFFSET_LOW_BYTE];
        final byte higher = buffer[byteOffset + OFFSET_HIGH_BYTE];

        final Integer i = (lower & 0xFF) | ((higher << 8) & 0xFF00);

        return targetClass.cast(i.shortValue());

    }

    @Override
    public S7Type getS7Type() {
        return S7Type.INT;
    }

    @Override
    public int getSizeInBits() {
        return 0;
    }

    @Override
    public int getSizeInBytes() {
        return 2;
    }

    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final Short value = (Short) javaType;
        final byte lower = (byte) ((value) & 0xFF);
        final byte higher = (byte) ((value >> 8) & 0xFF);
        buffer[byteOffset + OFFSET_LOW_BYTE] = lower;
        buffer[byteOffset + OFFSET_HIGH_BYTE] = higher;
    }

}
