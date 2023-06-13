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

import java.nio.charset.Charset;

/**
 * <p>StringLocator class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class StringLocator extends BaseLocator<String> {
    /**
     * Constant <code>ASCII</code>
     */
    public static final Charset ASCII = Charset.forName("ASCII");

    private final int dataType;
    private final int registerCount;
    private final Charset charset;

    /**
     * <p>Constructor for StringLocator.</p>
     *
     * @param slaveId       a int.
     * @param range         a int.
     * @param offset        a int.
     * @param dataType      a int.
     * @param registerCount a int.
     */
    public StringLocator(int slaveId, int range, int offset, int dataType, int registerCount) {
        this(slaveId, range, offset, dataType, registerCount, ASCII);
    }

    /**
     * <p>Constructor for StringLocator.</p>
     *
     * @param slaveId       a int.
     * @param range         a int.
     * @param offset        a int.
     * @param dataType      a int.
     * @param registerCount a int.
     * @param charset       a {@link Charset} object.
     */
    public StringLocator(int slaveId, int range, int offset, int dataType, int registerCount, Charset charset) {
        super(slaveId, range, offset);
        this.dataType = dataType;
        this.registerCount = registerCount;
        this.charset = charset;
        validate();
    }

    private void validate() {
        super.validate(registerCount);

        if (range == RegisterRange.COIL_STATUS || range == RegisterRange.INPUT_STATUS)
            throw new IllegalDataTypeException("Only binary values can be read from Coil and Input ranges");

        if (dataType != DataType.CHAR && dataType != DataType.VARCHAR)
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
     * {@inheritDoc}
     */
    @Override
    public int getRegisterCount() {
        return registerCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "StringLocator(slaveId=" + getSlaveId() + ", range=" + range + ", offset=" + offset + ", dataType="
                + dataType + ", registerCount=" + registerCount + ", charset=" + charset + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String bytesToValueRealOffset(byte[] data, int offset) {
        offset *= 2;
        int length = registerCount * 2;

        if (dataType == DataType.CHAR)
            return new String(data, offset, length, charset);

        if (dataType == DataType.VARCHAR) {
            int nullPos = -1;
            for (int i = offset; i < offset + length; i++) {
                if (data[i] == 0) {
                    nullPos = i;
                    break;
                }
            }

            if (nullPos == -1)
                return new String(data, offset, length, charset);
            return new String(data, offset, nullPos, charset);
        }

        throw new RuntimeException("Unsupported data type: " + dataType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short[] valueToShorts(String value) {
        short[] result = new short[registerCount];
        int resultByteLen = registerCount * 2;

        int length;
        if (value != null) {
            byte[] bytes = value.getBytes(charset);

            length = resultByteLen;
            if (length > bytes.length)
                length = bytes.length;

            for (int i = 0; i < length; i++)
                setByte(result, i, bytes[i] & 0xff);
        } else
            length = 0;

        if (dataType == DataType.CHAR) {
            // Pad the rest with spaces
            for (int i = length; i < resultByteLen; i++)
                setByte(result, i, 0x20);
        } else if (dataType == DataType.VARCHAR) {
            if (length >= resultByteLen)
                // Ensure the last byte is a null terminator.
                result[registerCount - 1] &= 0xff00;
            else {
                // Pad the rest with null.
                for (int i = length; i < resultByteLen; i++)
                    setByte(result, i, 0);
            }
        } else
            throw new RuntimeException("Unsupported data type: " + dataType);

        return result;
    }

    private void setByte(short[] s, int byteIndex, int value) {
        if (byteIndex % 2 == 0)
            s[byteIndex / 2] |= value << 8;
        else
            s[byteIndex / 2] |= value;
    }
    //
    //    public static void main(String[] args) {
    //        StringLocator l1 = new StringLocator(1, RegisterRange.HOLDING_REGISTER, 0, DataType.CHAR, 4);
    //        StringLocator l2 = new StringLocator(1, RegisterRange.HOLDING_REGISTER, 0, DataType.VARCHAR, 4);
    //
    //        short[] s;
    //
    //        s = l1.valueToShorts("abcdefg");
    //        System.out.println(new String(l1.bytesToValue(toBytes(s), 0)));
    //
    //        s = l1.valueToShorts("abcdefgh");
    //        System.out.println(new String(l1.bytesToValue(toBytes(s), 0)));
    //
    //        s = l1.valueToShorts("abcdefghi");
    //        System.out.println(new String(l1.bytesToValue(toBytes(s), 0)));
    //
    //        s = l2.valueToShorts("abcdef");
    //        System.out.println(new String(l2.bytesToValue(toBytes(s), 0)));
    //
    //        s = l2.valueToShorts("abcdefg");
    //        System.out.println(new String(l2.bytesToValue(toBytes(s), 0)));
    //
    //        s = l2.valueToShorts("abcdefgh");
    //        System.out.println(new String(l2.bytesToValue(toBytes(s), 0)));
    //
    //        s = l2.valueToShorts("abcdefghi");
    //        System.out.println(new String(l2.bytesToValue(toBytes(s), 0)));
    //    }
    //
    //    private static byte[] toBytes(short[] s) {
    //        byte[] b = new byte[s.length * 2];
    //        for (int i = 0; i < s.length; i++) {
    //            b[i * 2] = (byte) ((s[i] >> 8) & 0xff);
    //            b[i * 2 + 1] = (byte) (s[i] & 0xff);
    //        }
    //        return b;
    //    }
}
