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

import io.github.pnoker.driver.api.S7Type;

import java.util.Calendar;
import java.util.Date;

public final class DateConverter extends IntegerConverter {

    private static final long MILLI_TO_DAY_FACTOR = 24 * 60 * 60 * 1000;

    /**
     * 1.1.1990
     */
    private static final long OFFSET_1990;

    static {
        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.YEAR, 1990);

        OFFSET_1990 = c.getTime().getTime();
    }


    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final long days = super.extract(Integer.class, buffer, byteOffset, bitOffset);

        long millis = days * MILLI_TO_DAY_FACTOR;

        millis += OFFSET_1990;

        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.MILLISECOND, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.HOUR_OF_DAY, 0);

        return targetClass.cast(c.getTime());
    }


    @Override
    public S7Type getS7Type() {
        return S7Type.DATE;
    }


    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final Date d = (Date) javaType;

        long millis = d.getTime();

        millis -= OFFSET_1990;

        final double days = (double) millis / (double) MILLI_TO_DAY_FACTOR;

        final long ROUND = 1000;

        final long expected = (long) ((days * MILLI_TO_DAY_FACTOR) / ROUND);
        final long actual = millis / ROUND;

        if (expected != actual) {
            throw new IllegalArgumentException("Expected: " + expected + " got: " + actual);
        }

        if (millis < 0) {
            super.insert(0, buffer, byteOffset, bitOffset, 2);
        } else {
            super.insert((int) Math.round(days), buffer, byteOffset, bitOffset, 2);
        }
    }

}
