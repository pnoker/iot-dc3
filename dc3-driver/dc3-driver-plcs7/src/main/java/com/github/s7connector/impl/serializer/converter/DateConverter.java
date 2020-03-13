/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.impl.serializer.converter;

import com.github.s7connector.api.S7Type;

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

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public S7Type getS7Type() {
        return S7Type.DATE;
    }

    /**
     * {@inheritDoc}
     */
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
