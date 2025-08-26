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

/**
 * The Class BitConverter is responsible for converting bit values
 */
public final class BitConverter implements S7Serializable {


    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final byte bufValue = buffer[byteOffset];
        return targetClass.cast(bufValue == (bufValue | (0x01 << bitOffset)));
    }


    @Override
    public S7Type getS7Type() {
        return S7Type.BOOL;
    }


    @Override
    public int getSizeInBits() {
        return 1;
    }


    @Override
    public int getSizeInBytes() {
        return 0;
    }


    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final Boolean value = (Boolean) javaType;

        //thx to @mfriedemann (https://github.com/mfriedemann)
        if (value) {
            buffer[byteOffset] |= (0x01 << bitOffset);
        } else {
            buffer[byteOffset] &= ~(0x01 << bitOffset);
        }
    }

}
