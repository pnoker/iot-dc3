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

public final class DateAndTimeConverter extends ByteConverter {

    public static final int OFFSET_DAY = 2;
    public static final int OFFSET_HOUR = 3;
    public static final int OFFSET_MILLIS_1_AND_DOW = 7;
    public static final int OFFSET_MILLIS_100_10 = 6;
    public static final int OFFSET_MINUTE = 4;
    public static final int OFFSET_MONTH = 1;
    public static final int OFFSET_SECOND = 5;
    public static final int OFFSET_YEAR = 0;

    // 18, 1,16,16, 5,80,0,3, (dec)
    // 12, 1,10,10, 5,50,0,3, (hex)
    // 12-01-10 10:05:50.000

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T extract(final Class<T> targetClass, final byte[] buffer, final int byteOffset, final int bitOffset) {
        final Calendar c = Calendar.getInstance();
        c.clear();

        int year = this.getFromPLC(buffer, OFFSET_YEAR + byteOffset);

        if (year < 90) {
            // 1900 - 1989
            year += 2000;
        } else {
            // 2000 - 2090
            year += 1900;
        }

        int month = this.getFromPLC(buffer, OFFSET_MONTH + byteOffset);

        if (month > 0) {
            month--;
        }

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, this.getFromPLC(buffer, OFFSET_DAY + byteOffset));
        c.set(Calendar.HOUR_OF_DAY, this.getFromPLC(buffer, OFFSET_HOUR + byteOffset));
        c.set(Calendar.MINUTE, this.getFromPLC(buffer, OFFSET_MINUTE + byteOffset));
        c.set(Calendar.SECOND, this.getFromPLC(buffer, OFFSET_SECOND + byteOffset));

        /*
         * TODO byte upperMillis = super.extract(Byte.class, buffer,
         * OFFSET_MILLIS_100_10+byteOffset, bitOffset); byte lowerMillis =
         * super.extract(Byte.class, buffer, OFFSET_MILLIS_1_AND_DOW+byteOffset,
         * bitOffset);
         *
         * int ms100 = ( upperMillis >> 4 ); int ms10 = ( upperMillis & 0x0F );
         * int ms1 = ( lowerMillis >> 4 );
         *
         * int millis = ms1 + ( 10*ms10 ) + ( 100*ms100 );
         * c.set(Calendar.MILLISECOND, millis);
         *
         * int dow = ( lowerMillis & 0x0F ); c.set(Calendar.DAY_OF_WEEK, dow);
         */

        return targetClass.cast(c.getTime());
    }

    /**
     * Dec -> Hex 10 = 0a 16 = 0f 17 = 10
     *
     * @param buffer
     * @param offset
     * @return
     */
    public byte getFromPLC(final byte[] buffer, final int offset) {
        try {
            final byte ret = super.extract(Byte.class, buffer, offset, 0);
            return (byte) Integer.parseInt(Integer.toHexString(ret & 0xFF));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public S7Type getS7Type() {
        return S7Type.DATE_AND_TIME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSizeInBits() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getSizeInBytes() {
        return 8;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final Object javaType, final byte[] buffer, final int byteOffset, final int bitOffset,
                       final int size) {
        final Date date = (Date) javaType;
        final Calendar c = Calendar.getInstance();
        c.setTime(date);

        int year = c.get(Calendar.YEAR);

        /*
         * if (year < 1990 || year > 2090) throw new
         * S7Exception("Invalid year: " + year + " @ offset: " + byteOffset);
         */

        if (year < 2000) {
            // 1990 -1999
            year -= 1900;
        } else {
            // 2000 - 2089
            year -= 2000;
        }

        this.putToPLC(buffer, byteOffset + OFFSET_YEAR, year);
        this.putToPLC(buffer, byteOffset + OFFSET_MONTH, c.get(Calendar.MONTH) + 1);
        this.putToPLC(buffer, byteOffset + OFFSET_DAY, c.get(Calendar.DAY_OF_MONTH));
        this.putToPLC(buffer, byteOffset + OFFSET_HOUR, c.get(Calendar.HOUR_OF_DAY));
        this.putToPLC(buffer, byteOffset + OFFSET_MINUTE, c.get(Calendar.MINUTE));
        this.putToPLC(buffer, byteOffset + OFFSET_SECOND, c.get(Calendar.SECOND));

        /*
         * TODO int msec1 = 0, msec10 = 0, msec100 = 0; Integer millis =
         * c.get(Calendar.MILLISECOND); String mStr = millis.toString();
         *
         * if (mStr.length() > 2) { msec100 = Integer.parseInt(
         * mStr.substring(0, 1) ); msec10 = Integer.parseInt( mStr.substring(1,
         * 2) ); msec1 = Integer.parseInt( mStr.substring(2, 3) ); } else if
         * (mStr.length() > 1) { msec10 = Integer.parseInt( mStr.substring(0, 1)
         * ); msec1 = Integer.parseInt( mStr.substring(1, 2) ); } else { msec1 =
         * Integer.parseInt( mStr.substring(0, 1) ); }
         *
         * super.insert( (byte)( (byte)msec10 | (byte)(msec100 << 4) ), buffer,
         * OFFSET_MILLIS_100_10+byteOffset, 0, 1);
         *
         * int dow = c.get(Calendar.DAY_OF_WEEK);
         *
         * super.insert( (byte)( (byte)dow | (byte)(msec1 << 4) ), buffer,
         * OFFSET_MILLIS_1_AND_DOW+byteOffset, 0, 1);
         */
    }

    /**
     * Hex -> dec 0a = 10 0f = 16 10 = 17
     *
     * @param buffer
     * @param offset
     * @param i
     */
    public void putToPLC(final byte[] buffer, final int offset, final int i) {
        try {
            final int ret = Integer.parseInt("" + i, 16);
            buffer[offset] = (byte) ret;
        } catch (final NumberFormatException e) {
            return;
        }
    }

}
