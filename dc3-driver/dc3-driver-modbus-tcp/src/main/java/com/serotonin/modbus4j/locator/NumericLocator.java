/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.serotonin.modbus4j.locator;

import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.IllegalDataTypeException;
import org.apache.commons.lang3.ArrayUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * <p>NumericLocator class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class NumericLocator extends BaseLocator<Number> {
    private static final int[] DATA_TYPES = { //
            DataType.TWO_BYTE_INT_UNSIGNED, //
            DataType.TWO_BYTE_INT_SIGNED, //
            DataType.TWO_BYTE_INT_UNSIGNED_SWAPPED, //
            DataType.TWO_BYTE_INT_SIGNED_SWAPPED, //
            DataType.FOUR_BYTE_INT_UNSIGNED, //
            DataType.FOUR_BYTE_INT_SIGNED, //
            DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED, //
            DataType.FOUR_BYTE_INT_SIGNED_SWAPPED, //
            DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED, //
            DataType.FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED, //
            DataType.FOUR_BYTE_FLOAT, //
            DataType.FOUR_BYTE_FLOAT_SWAPPED, //
            DataType.EIGHT_BYTE_INT_UNSIGNED, //
            DataType.EIGHT_BYTE_INT_SIGNED, //
            DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED, //
            DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED, //
            DataType.EIGHT_BYTE_FLOAT, //
            DataType.EIGHT_BYTE_FLOAT_SWAPPED, //
            DataType.TWO_BYTE_BCD, //
            DataType.FOUR_BYTE_BCD, //
            DataType.FOUR_BYTE_BCD_SWAPPED, //
            DataType.FOUR_BYTE_MOD_10K, //
            DataType.FOUR_BYTE_MOD_10K_SWAPPED, //
            DataType.SIX_BYTE_MOD_10K,
            DataType.SIX_BYTE_MOD_10K_SWAPPED,
            DataType.EIGHT_BYTE_MOD_10K, //
            DataType.EIGHT_BYTE_MOD_10K_SWAPPED, //
            DataType.ONE_BYTE_INT_UNSIGNED_LOWER, //
            DataType.ONE_BYTE_INT_UNSIGNED_UPPER
    };

    private final int dataType;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;

    /**
     * <p>Constructor for NumericLocator.</p>
     *
     * @param slaveId  a int.
     * @param range    a int.
     * @param offset   a int.
     * @param dataType a int.
     */
    public NumericLocator(int slaveId, int range, int offset, int dataType) {
        super(slaveId, range, offset);
        this.dataType = dataType;
        validate();
    }

    private void validate() {
        super.validate(getRegisterCount());

        if (range == RegisterRange.COIL_STATUS || range == RegisterRange.INPUT_STATUS)
            throw new IllegalDataTypeException("Only binary values can be read from Coil and Input ranges");

        if (!ArrayUtils.contains(DATA_TYPES, dataType))
            throw new IllegalDataTypeException("Invalid data type");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getDataType() {
        return dataType;
    }

    /**
     * <p>Getter for the field <code>roundingMode</code>.</p>
     *
     * @return a {@link RoundingMode} object.
     */
    public RoundingMode getRoundingMode() {
        return roundingMode;
    }

    /**
     * <p>Setter for the field <code>roundingMode</code>.</p>
     *
     * @param roundingMode a {@link RoundingMode} object.
     */
    public void setRoundingMode(RoundingMode roundingMode) {
        this.roundingMode = roundingMode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "NumericLocator(slaveId=" + getSlaveId() + ", range=" + range + ", offset=" + offset + ", dataType="
                + dataType + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRegisterCount() {
        switch (dataType) {
            case DataType.TWO_BYTE_INT_UNSIGNED:
            case DataType.TWO_BYTE_INT_SIGNED:
            case DataType.TWO_BYTE_INT_UNSIGNED_SWAPPED:
            case DataType.TWO_BYTE_INT_SIGNED_SWAPPED:
            case DataType.TWO_BYTE_BCD:
            case DataType.ONE_BYTE_INT_UNSIGNED_LOWER:
            case DataType.ONE_BYTE_INT_UNSIGNED_UPPER:
                return 1;
            case DataType.FOUR_BYTE_INT_UNSIGNED:
            case DataType.FOUR_BYTE_INT_SIGNED:
            case DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED:
            case DataType.FOUR_BYTE_INT_SIGNED_SWAPPED:
            case DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED:
            case DataType.FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED:
            case DataType.FOUR_BYTE_FLOAT:
            case DataType.FOUR_BYTE_FLOAT_SWAPPED:
            case DataType.FOUR_BYTE_BCD:
            case DataType.FOUR_BYTE_BCD_SWAPPED:
            case DataType.FOUR_BYTE_MOD_10K:
            case DataType.FOUR_BYTE_MOD_10K_SWAPPED:
                return 2;
            case DataType.SIX_BYTE_MOD_10K:
            case DataType.SIX_BYTE_MOD_10K_SWAPPED:
                return 3;
            case DataType.EIGHT_BYTE_INT_UNSIGNED:
            case DataType.EIGHT_BYTE_INT_SIGNED:
            case DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED:
            case DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED:
            case DataType.EIGHT_BYTE_FLOAT:
            case DataType.EIGHT_BYTE_FLOAT_SWAPPED:
            case DataType.EIGHT_BYTE_MOD_10K:
            case DataType.EIGHT_BYTE_MOD_10K_SWAPPED:
                return 4;
        }

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Number bytesToValueRealOffset(byte[] data, int offset) {
        offset *= 2;

        // 2 bytes
        if (dataType == DataType.TWO_BYTE_INT_UNSIGNED)
            return new Integer(((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.TWO_BYTE_INT_SIGNED)
            return new Short((short) (((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff)));

        if (dataType == DataType.TWO_BYTE_INT_UNSIGNED_SWAPPED)
            return new Integer(((data[offset + 1] & 0xff) << 8) | (data[offset] & 0xff));

        if (dataType == DataType.TWO_BYTE_INT_SIGNED_SWAPPED)
            return new Short((short) (((data[offset + 1] & 0xff) << 8) | (data[offset] & 0xff)));

        if (dataType == DataType.TWO_BYTE_BCD) {
            StringBuilder sb = new StringBuilder();
            appendBCD(sb, data[offset]);
            appendBCD(sb, data[offset + 1]);
            return Short.parseShort(sb.toString());
        }

        // 1 byte
        if (dataType == DataType.ONE_BYTE_INT_UNSIGNED_LOWER)
            return new Integer(data[offset + 1] & 0xff);
        if (dataType == DataType.ONE_BYTE_INT_UNSIGNED_UPPER)
            return new Integer(data[offset] & 0xff);

        // 4 bytes
        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED)
            return new Long(((long) ((data[offset] & 0xff)) << 24) | ((long) ((data[offset + 1] & 0xff)) << 16)
                    | ((long) ((data[offset + 2] & 0xff)) << 8) | ((data[offset + 3] & 0xff)));

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED)
            return new Integer(((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff));

        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED)
            return new Long(((long) ((data[offset + 2] & 0xff)) << 24) | ((long) ((data[offset + 3] & 0xff)) << 16)
                    | ((long) ((data[offset] & 0xff)) << 8) | ((data[offset + 1] & 0xff)));

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED)
            return new Integer(((data[offset + 2] & 0xff) << 24) | ((data[offset + 3] & 0xff) << 16)
                    | ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED)
            return new Long(((long) ((data[offset + 3] & 0xff)) << 24) | (((data[offset + 2] & 0xff) << 16))
                    | ((long) ((data[offset + 1] & 0xff)) << 8) | (data[offset] & 0xff));

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED)
            return new Integer(((data[offset + 3] & 0xff) << 24) | ((data[offset + 2] & 0xff) << 16)
                    | ((data[offset + 1] & 0xff) << 8) | ((data[offset] & 0xff)));

        if (dataType == DataType.FOUR_BYTE_FLOAT)
            return Float.intBitsToFloat(((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16)
                    | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff));

        if (dataType == DataType.FOUR_BYTE_FLOAT_SWAPPED)
            return Float.intBitsToFloat(((data[offset + 2] & 0xff) << 24) | ((data[offset + 3] & 0xff) << 16)
                    | ((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff));

        if (dataType == DataType.FOUR_BYTE_BCD) {
            StringBuilder sb = new StringBuilder();
            appendBCD(sb, data[offset]);
            appendBCD(sb, data[offset + 1]);
            appendBCD(sb, data[offset + 2]);
            appendBCD(sb, data[offset + 3]);
            return Integer.parseInt(sb.toString());
        }

        if (dataType == DataType.FOUR_BYTE_BCD_SWAPPED) {
            StringBuilder sb = new StringBuilder();
            appendBCD(sb, data[offset + 2]);
            appendBCD(sb, data[offset + 3]);
            appendBCD(sb, data[offset]);
            appendBCD(sb, data[offset + 1]);
            return Integer.parseInt(sb.toString());
        }

        //MOD10K types
        if (dataType == DataType.FOUR_BYTE_MOD_10K_SWAPPED)
            return BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))).multiply(BigInteger.valueOf(10000L))
                    .add(BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))));
        if (dataType == DataType.FOUR_BYTE_MOD_10K)
            return BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))).multiply(BigInteger.valueOf(10000L))
                    .add(BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))));
        if (dataType == DataType.SIX_BYTE_MOD_10K_SWAPPED)
            return BigInteger.valueOf((((data[offset + 4] & 0xff) << 8) + (data[offset + 5] & 0xff))).multiply(BigInteger.valueOf(100000000L))
                    .add(BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))).multiply(BigInteger.valueOf(10000L)))
                    .add(BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))));
        if (dataType == DataType.SIX_BYTE_MOD_10K)
            return BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))).multiply(BigInteger.valueOf(100000000L))
                    .add(BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))).multiply(BigInteger.valueOf(10000L)))
                    .add(BigInteger.valueOf((((data[offset + 4] & 0xff) << 8) + (data[offset + 5] & 0xff))));
        if (dataType == DataType.EIGHT_BYTE_MOD_10K_SWAPPED)
            return BigInteger.valueOf((((data[offset + 6] & 0xff) << 8) + (data[offset + 7] & 0xff))).multiply(BigInteger.valueOf(1000000000000L))
                    .add(BigInteger.valueOf((((data[offset + 4] & 0xff) << 8) + (data[offset + 5] & 0xff))).multiply(BigInteger.valueOf(100000000L)))
                    .add(BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))).multiply(BigInteger.valueOf(10000L)))
                    .add(BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))));
        if (dataType == DataType.EIGHT_BYTE_MOD_10K)
            return BigInteger.valueOf((((data[offset] & 0xff) << 8) + (data[offset + 1] & 0xff))).multiply(BigInteger.valueOf(1000000000000L))
                    .add(BigInteger.valueOf((((data[offset + 2] & 0xff) << 8) + (data[offset + 3] & 0xff))).multiply(BigInteger.valueOf(100000000L)))
                    .add(BigInteger.valueOf((((data[offset + 4] & 0xff) << 8) + (data[offset + 5] & 0xff))).multiply(BigInteger.valueOf(10000L)))
                    .add(BigInteger.valueOf((((data[offset + 6] & 0xff) << 8) + (data[offset + 7] & 0xff))));

        // 8 bytes
        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED) {
            byte[] b9 = new byte[9];
            System.arraycopy(data, offset, b9, 1, 8);
            return new BigInteger(b9);
        }

        if (dataType == DataType.EIGHT_BYTE_INT_SIGNED)
            return new Long(((long) ((data[offset] & 0xff)) << 56) | ((long) ((data[offset + 1] & 0xff)) << 48)
                    | ((long) ((data[offset + 2] & 0xff)) << 40) | ((long) ((data[offset + 3] & 0xff)) << 32)
                    | ((long) ((data[offset + 4] & 0xff)) << 24) | ((long) ((data[offset + 5] & 0xff)) << 16)
                    | ((long) ((data[offset + 6] & 0xff)) << 8) | ((data[offset + 7] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED) {
            byte[] b9 = new byte[9];
            b9[1] = data[offset + 6];
            b9[2] = data[offset + 7];
            b9[3] = data[offset + 4];
            b9[4] = data[offset + 5];
            b9[5] = data[offset + 2];
            b9[6] = data[offset + 3];
            b9[7] = data[offset];
            b9[8] = data[offset + 1];
            return new BigInteger(b9);
        }

        if (dataType == DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED)
            return new Long(((long) ((data[offset + 6] & 0xff)) << 56) | ((long) ((data[offset + 7] & 0xff)) << 48)
                    | ((long) ((data[offset + 4] & 0xff)) << 40) | ((long) ((data[offset + 5] & 0xff)) << 32)
                    | ((long) ((data[offset + 2] & 0xff)) << 24) | ((long) ((data[offset + 3] & 0xff)) << 16)
                    | ((long) ((data[offset] & 0xff)) << 8) | ((data[offset + 1] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_FLOAT)
            return Double.longBitsToDouble(((long) ((data[offset] & 0xff)) << 56)
                    | ((long) ((data[offset + 1] & 0xff)) << 48) | ((long) ((data[offset + 2] & 0xff)) << 40)
                    | ((long) ((data[offset + 3] & 0xff)) << 32) | ((long) ((data[offset + 4] & 0xff)) << 24)
                    | ((long) ((data[offset + 5] & 0xff)) << 16) | ((long) ((data[offset + 6] & 0xff)) << 8)
                    | ((data[offset + 7] & 0xff)));

        if (dataType == DataType.EIGHT_BYTE_FLOAT_SWAPPED)
            return Double.longBitsToDouble(((long) ((data[offset + 6] & 0xff)) << 56)
                    | ((long) ((data[offset + 7] & 0xff)) << 48) | ((long) ((data[offset + 4] & 0xff)) << 40)
                    | ((long) ((data[offset + 5] & 0xff)) << 32) | ((long) ((data[offset + 2] & 0xff)) << 24)
                    | ((long) ((data[offset + 3] & 0xff)) << 16) | ((long) ((data[offset] & 0xff)) << 8)
                    | ((data[offset + 1] & 0xff)));

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    private static void appendBCD(StringBuilder sb, byte b) {
        sb.append(bcdNibbleToInt(b, true));
        sb.append(bcdNibbleToInt(b, false));
    }

    private static int bcdNibbleToInt(byte b, boolean high) {
        int n;
        if (high)
            n = (b >> 4) & 0xf;
        else
            n = b & 0xf;
        if (n > 9)
            n = 0;
        return n;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short[] valueToShorts(Number value) {
        // 2 bytes
        if (dataType == DataType.TWO_BYTE_INT_UNSIGNED || dataType == DataType.TWO_BYTE_INT_SIGNED)
            return new short[]{toShort(value)};

        if (dataType == DataType.TWO_BYTE_INT_SIGNED_SWAPPED || dataType == DataType.TWO_BYTE_INT_UNSIGNED_SWAPPED) {
            short sval = toShort(value);
            //0x1100
            return new short[]{(short) (((sval & 0xFF00) >> 8) | ((sval & 0x00FF) << 8))};
        }

        if (dataType == DataType.TWO_BYTE_BCD) {
            short s = toShort(value);
            return new short[]{(short) ((((s / 1000) % 10) << 12) | (((s / 100) % 10) << 8) | (((s / 10) % 10) << 4) | (s % 10))};
        }

        if (dataType == DataType.ONE_BYTE_INT_UNSIGNED_LOWER) {
            return new short[]{(short) (toShort(value) & 0x00FF)};
        }
        if (dataType == DataType.ONE_BYTE_INT_UNSIGNED_UPPER) {
            return new short[]{(short) ((toShort(value) << 8) & 0xFF00)};
        }

        // 4 bytes
        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED || dataType == DataType.FOUR_BYTE_INT_SIGNED) {
            int i = toInt(value);
            return new short[]{(short) (i >> 16), (short) i};
        }

        if (dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED || dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED) {
            int i = toInt(value);
            return new short[]{(short) i, (short) (i >> 16)};
        }

        if (dataType == DataType.FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED
                || dataType == DataType.FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED) {
            int i = toInt(value);
            short topWord = (short) (((i & 0xFF) << 8) | ((i >> 8) & 0xFF));
            short bottomWord = (short) (((i >> 24) & 0x000000FF) | ((i >> 8) & 0x0000FF00));
            return new short[]{topWord, bottomWord};
        }

        if (dataType == DataType.FOUR_BYTE_FLOAT) {
            int i = Float.floatToIntBits(value.floatValue());
            return new short[]{(short) (i >> 16), (short) i};
        }

        if (dataType == DataType.FOUR_BYTE_FLOAT_SWAPPED) {
            int i = Float.floatToIntBits(value.floatValue());
            return new short[]{(short) i, (short) (i >> 16)};
        }

        if (dataType == DataType.FOUR_BYTE_BCD) {
            int i = toInt(value);
            return new short[]{
                    (short) ((((i / 10000000) % 10) << 12) | (((i / 1000000) % 10) << 8) | (((i / 100000) % 10) << 4) | ((i / 10000) % 10)),
                    (short) ((((i / 1000) % 10) << 12) | (((i / 100) % 10) << 8) | (((i / 10) % 10) << 4) | (i % 10))};
        }

        // MOD10K
        if (dataType == DataType.FOUR_BYTE_MOD_10K) {
            long l = value.longValue();
            return new short[]{(short) ((l / 10000) % 10000), (short) (l % 10000)};
        }
        if (dataType == DataType.FOUR_BYTE_MOD_10K_SWAPPED) {
            long l = value.longValue();
            return new short[]{(short) (l % 10000), (short) ((l / 10000) % 10000)};
        }
        if (dataType == DataType.SIX_BYTE_MOD_10K) {
            long l = value.longValue();
            return new short[]{(short) ((l / 100000000L) % 10000), (short) ((l / 10000) % 10000), (short) (l % 10000)};
        }
        if (dataType == DataType.SIX_BYTE_MOD_10K_SWAPPED) {
            long l = value.longValue();
            return new short[]{(short) (l % 10000), (short) ((l / 10000) % 10000), (short) ((l / 100000000L) % 10000)};
        }
        if (dataType == DataType.EIGHT_BYTE_MOD_10K) {
            long l = value.longValue();
            return new short[]{(short) ((l / 1000000000000L) % 10000), (short) ((l / 100000000L) % 10000), (short) ((l / 10000) % 10000), (short) (l % 10000)};
        }
        if (dataType == DataType.EIGHT_BYTE_MOD_10K_SWAPPED) {
            long l = value.longValue();
            return new short[]{(short) (l % 10000), (short) ((l / 10000) % 10000), (short) ((l / 100000000L) % 10000), (short) ((l / 1000000000000L) % 10000)};
        }

        // 8 bytes
        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED || dataType == DataType.EIGHT_BYTE_INT_SIGNED) {
            long l = value.longValue();
            return new short[]{(short) (l >> 48), (short) (l >> 32), (short) (l >> 16), (short) l};
        }

        if (dataType == DataType.EIGHT_BYTE_INT_UNSIGNED_SWAPPED || dataType == DataType.EIGHT_BYTE_INT_SIGNED_SWAPPED) {
            long l = value.longValue();
            return new short[]{(short) l, (short) (l >> 16), (short) (l >> 32), (short) (l >> 48)};
        }

        if (dataType == DataType.EIGHT_BYTE_FLOAT) {
            long l = Double.doubleToLongBits(value.doubleValue());
            return new short[]{(short) (l >> 48), (short) (l >> 32), (short) (l >> 16), (short) l};
        }

        if (dataType == DataType.EIGHT_BYTE_FLOAT_SWAPPED) {
            long l = Double.doubleToLongBits(value.doubleValue());
            return new short[]{(short) l, (short) (l >> 16), (short) (l >> 32), (short) (l >> 48)};
        }

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    private short toShort(Number value) {
        return (short) toInt(value);
    }

    private int toInt(Number value) {
        if (value instanceof Double)
            return new BigDecimal(value.doubleValue()).setScale(0, roundingMode).intValue();
        if (value instanceof Float)
            return new BigDecimal(value.floatValue()).setScale(0, roundingMode).intValue();
        if (value instanceof BigDecimal)
            return ((BigDecimal) value).setScale(0, roundingMode).intValue();
        return value.intValue();
    }
}
