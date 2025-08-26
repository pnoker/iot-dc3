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

public final class RealConverter implements S7Serializable {

    private static final int OFFSET_POS1 = 0;
    private static final int OFFSET_POS2 = 1;
    private static final int OFFSET_POS3 = 2;
    private static final int OFFSET_POS4 = 3;

    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final int iValue = ((buffer[byteOffset + OFFSET_POS4] & 0xFF))
                | ((buffer[byteOffset + OFFSET_POS3] & 0xFF) << 8) | ((buffer[byteOffset + OFFSET_POS2] & 0xFF) << 16)
                | ((buffer[byteOffset + OFFSET_POS1] & 0xFF) << 24);

        final Float fValue = Float.intBitsToFloat(iValue);

        Object ret = fValue;

        if (targetClass == Double.class) {
            ret = Double.parseDouble(fValue.toString());
        }

        return targetClass.cast(ret);
    }

    @Override
    public S7Type getS7Type() {
        return S7Type.REAL;
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
        final float fValue = Float.parseFloat(javaType.toString());

        final int iValue = Float.floatToIntBits(fValue);

        buffer[byteOffset + OFFSET_POS4] = (byte) ((iValue) & 0xFF);
        buffer[byteOffset + OFFSET_POS3] = (byte) ((iValue >> 8) & 0xFF);
        buffer[byteOffset + OFFSET_POS2] = (byte) ((iValue >> 16) & 0xFF);
        buffer[byteOffset + OFFSET_POS1] = (byte) ((iValue >> 24) & 0xFF);
    }

}
