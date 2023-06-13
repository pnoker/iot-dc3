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
package com.serotonin.modbus4j.value;

import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.exception.InvalidDataConversionException;

import java.math.BigInteger;

/**
 * <p>Abstract ModbusValue class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
abstract public class ModbusValue {
    private final DataType type;
    private final Object value;

    /**
     * <p>Constructor for ModbusValue.</p>
     *
     * @param type  a {@link DataType} object.
     * @param value a {@link Object} object.
     */
    public ModbusValue(DataType type, Object value) {
        this.type = type;
        this.value = value;
    }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link DataType} object.
     */
    public DataType getType() {
        return type;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a {@link Object} object.
     */
    public Object getValue() {
        return value;
    }

    /**
     * <p>booleanValue.</p>
     *
     * @return a boolean.
     */
    public boolean booleanValue() {
        if (value instanceof Boolean)
            return ((Boolean) value).booleanValue();
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to boolean");
    }

    /**
     * <p>intValue.</p>
     *
     * @return a int.
     */
    public int intValue() {
        if (value instanceof Integer)
            return ((Integer) value).intValue();
        if (value instanceof Short)
            return ((Short) value).shortValue() & 0xffff;
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to int");
    }

    /**
     * <p>longValue.</p>
     *
     * @return a long.
     */
    public long longValue() {
        if (value instanceof Long)
            return ((Long) value).longValue();
        if (value instanceof Integer)
            return ((Integer) value).intValue() & 0xffffffff;
        if (value instanceof Short)
            return ((Short) value).shortValue() & 0xffff;
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to long");
    }

    /**
     * <p>bigIntegerValue.</p>
     *
     * @return a {@link BigInteger} object.
     */
    public BigInteger bigIntegerValue() {
        if (value instanceof BigInteger)
            return (BigInteger) value;
        if (value instanceof Long)
            return BigInteger.valueOf(((Long) value).longValue());
        if (value instanceof Integer)
            return BigInteger.valueOf(((Integer) value).intValue() & 0xffffffff);
        if (value instanceof Short)
            return BigInteger.valueOf(((Short) value).shortValue() & 0xffff);
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to BigInteger");
    }

    /**
     * <p>floatValue.</p>
     *
     * @return a float.
     */
    public float floatValue() {
        if (value instanceof Float)
            return ((Float) value).floatValue();
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to float");
    }

    /**
     * <p>doubleValue.</p>
     *
     * @return a double.
     */
    public double doubleValue() {
        if (value instanceof Double)
            return ((Double) value).doubleValue();
        if (value instanceof Float)
            return ((Float) value).doubleValue();
        throw new InvalidDataConversionException("Can't convert " + value.getClass() + " to float");
    }
}
