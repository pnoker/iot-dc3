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

package com.serotonin.modbus4j.locator;

import com.serotonin.modbus4j.base.ModbusUtils;
import com.serotonin.modbus4j.base.RangeAndOffset;
import com.serotonin.modbus4j.code.DataType;
import com.serotonin.modbus4j.code.RegisterRange;
import com.serotonin.modbus4j.exception.ModbusIdException;
import com.serotonin.modbus4j.exception.ModbusTransportException;

import java.nio.charset.Charset;

/**
 * <p>Abstract BaseLocator class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
abstract public class BaseLocator<T> {
    //
    //
    // Factory methods
    //

    protected final int range;
    protected final int offset;
    private final int slaveId;

    /**
     * <p>Constructor for BaseLocator.</p>
     *
     * @param slaveId a int.
     * @param range   a int.
     * @param offset  a int.
     */
    public BaseLocator(int slaveId, int range, int offset) {
        this.slaveId = slaveId;
        this.range = range;
        this.offset = offset;
    }

    /**
     * <p>coilStatus.</p>
     *
     * @param slaveId a int.
     * @param offset  a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Boolean> coilStatus(int slaveId, int offset) {
        return new BinaryLocator(slaveId, RegisterRange.COIL_STATUS, offset);
    }

    /**
     * <p>inputStatus.</p>
     *
     * @param slaveId a int.
     * @param offset  a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Boolean> inputStatus(int slaveId, int offset) {
        return new BinaryLocator(slaveId, RegisterRange.INPUT_STATUS, offset);
    }

    /**
     * <p>inputRegister.</p>
     *
     * @param slaveId  a int.
     * @param offset   a int.
     * @param dataType a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Number> inputRegister(int slaveId, int offset, int dataType) {
        return new NumericLocator(slaveId, RegisterRange.INPUT_REGISTER, offset, dataType);
    }

    /**
     * <p>inputRegisterBit.</p>
     *
     * @param slaveId a int.
     * @param offset  a int.
     * @param bit     a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Boolean> inputRegisterBit(int slaveId, int offset, int bit) {
        return new BinaryLocator(slaveId, RegisterRange.INPUT_REGISTER, offset, bit);
    }

    /**
     * <p>holdingRegister.</p>
     *
     * @param slaveId  a int.
     * @param offset   a int.
     * @param dataType a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Number> holdingRegister(int slaveId, int offset, int dataType) {
        return new NumericLocator(slaveId, RegisterRange.HOLDING_REGISTER, offset, dataType);
    }

    /**
     * <p>holdingRegisterBit.</p>
     *
     * @param slaveId a int.
     * @param offset  a int.
     * @param bit     a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<Boolean> holdingRegisterBit(int slaveId, int offset, int bit) {
        return new BinaryLocator(slaveId, RegisterRange.HOLDING_REGISTER, offset, bit);
    }

    /**
     * <p>createLocator.</p>
     *
     * @param slaveId       a int.
     * @param registerId    a int.
     * @param dataType      a int.
     * @param bit           a int.
     * @param registerCount a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<?> createLocator(int slaveId, int registerId, int dataType, int bit, int registerCount) {
        RangeAndOffset rao = new RangeAndOffset(registerId);
        return createLocator(slaveId, rao.getRange(), rao.getOffset(), dataType, bit, registerCount,
                StringLocator.ASCII);
    }

    /**
     * <p>createLocator.</p>
     *
     * @param slaveId       a int.
     * @param registerId    a int.
     * @param dataType      a int.
     * @param bit           a int.
     * @param registerCount a int.
     * @param charset       a {@link Charset} object.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<?> createLocator(int slaveId, int registerId, int dataType, int bit, int registerCount,
                                               Charset charset) {
        RangeAndOffset rao = new RangeAndOffset(registerId);
        return createLocator(slaveId, rao.getRange(), rao.getOffset(), dataType, bit, registerCount, charset);
    }

    /**
     * <p>createLocator.</p>
     *
     * @param slaveId       a int.
     * @param range         a int.
     * @param offset        a int.
     * @param dataType      a int.
     * @param bit           a int.
     * @param registerCount a int.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<?> createLocator(int slaveId, int range, int offset, int dataType, int bit,
                                               int registerCount) {
        return createLocator(slaveId, range, offset, dataType, bit, registerCount, StringLocator.ASCII);
    }

    /**
     * <p>createLocator.</p>
     *
     * @param slaveId       a int.
     * @param range         a int.
     * @param offset        a int.
     * @param dataType      a int.
     * @param bit           a int.
     * @param registerCount a int.
     * @param charset       a {@link Charset} object.
     * @return a {@link BaseLocator} object.
     */
    public static BaseLocator<?> createLocator(int slaveId, int range, int offset, int dataType, int bit,
                                               int registerCount, Charset charset) {
        if (dataType == DataType.BINARY) {
            if (BinaryLocator.isBinaryRange(range))
                return new BinaryLocator(slaveId, range, offset);
            return new BinaryLocator(slaveId, range, offset, bit);
        }
        if (dataType == DataType.CHAR || dataType == DataType.VARCHAR)
            return new StringLocator(slaveId, range, offset, dataType, registerCount, charset);
        return new NumericLocator(slaveId, range, offset, dataType);
    }

    /**
     * <p>validate.</p>
     *
     * @param registerCount a int.
     */
    protected void validate(int registerCount) {
        try {
            ModbusUtils.validateOffset(offset);
            ModbusUtils.validateEndOffset(offset + registerCount - 1);
        } catch (ModbusTransportException e) {
            throw new ModbusIdException(e);
        }
    }

    /**
     * <p>getDataType.</p>
     *
     * @return a int.
     */
    abstract public int getDataType();

    /**
     * <p>getRegisterCount.</p>
     *
     * @return a int.
     */
    abstract public int getRegisterCount();

    /**
     * <p>Getter for the field <code>slaveId</code>.</p>
     *
     * @return a int.
     */
    public int getSlaveId() {
        return slaveId;
    }

    /**
     * <p>Getter for the field <code>range</code>.</p>
     *
     * @return a int.
     */
    public int getRange() {
        return range;
    }

    /**
     * <p>Getter for the field <code>offset</code>.</p>
     *
     * @return a int.
     */
    public int getOffset() {
        return offset;
    }

    //    public SlaveAndRange getSlaveAndRange() {
    //        return slaveAndRange;
    //    }

    /**
     * <p>getEndOffset.</p>
     *
     * @return a int.
     */
    public int getEndOffset() {
        return offset + getRegisterCount() - 1;
    }

    /**
     * <p>bytesToValue.</p>
     *
     * @param data          an array of {@link byte} objects.
     * @param requestOffset a int.
     * @return a T object.
     */
    public T bytesToValue(byte[] data, int requestOffset) {
        // Determined the offset normalized to the response data.
        return bytesToValueRealOffset(data, offset - requestOffset);
    }

    /**
     * <p>bytesToValueRealOffset.</p>
     *
     * @param data   an array of {@link byte} objects.
     * @param offset a int.
     * @return a T object.
     */
    abstract public T bytesToValueRealOffset(byte[] data, int offset);

    /**
     * <p>valueToShorts.</p>
     *
     * @param value a T object.
     * @return an array of {@link short} objects.
     */
    abstract public short[] valueToShorts(T value);
}
