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
package com.serotonin.modbus4j.base;

import com.serotonin.modbus4j.ExceptionResult;
import com.serotonin.modbus4j.code.ExceptionCode;
import com.serotonin.modbus4j.locator.BaseLocator;

/**
 * <p>KeyedModbusLocator class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class KeyedModbusLocator<K> {
    private final K key;
    private final BaseLocator<?> locator;

    /**
     * <p>Constructor for KeyedModbusLocator.</p>
     *
     * @param key     a K object.
     * @param locator a {@link BaseLocator} object.
     */
    public KeyedModbusLocator(K key, BaseLocator<?> locator) {
        this.key = key;
        this.locator = locator;
    }

    /**
     * <p>Getter for the field <code>key</code>.</p>
     *
     * @return a K object.
     */
    public K getKey() {
        return key;
    }

    /**
     * <p>Getter for the field <code>locator</code>.</p>
     *
     * @return a {@link BaseLocator} object.
     */
    public BaseLocator<?> getLocator() {
        return locator;
    }

    @Override
    public String toString() {
        return "KeyedModbusLocator(key=" + key + ", locator=" + locator + ")";
    }

    //
    ///
    /// Delegation.
    ///
    //

    /**
     * <p>getDataType.</p>
     *
     * @return a int.
     */
    public int getDataType() {
        return locator.getDataType();
    }

    /**
     * <p>getOffset.</p>
     *
     * @return a int.
     */
    public int getOffset() {
        return locator.getOffset();
    }

    /**
     * <p>getSlaveAndRange.</p>
     *
     * @return a {@link SlaveAndRange} object.
     */
    public SlaveAndRange getSlaveAndRange() {
        return new SlaveAndRange(locator.getSlaveId(), locator.getRange());
    }

    /**
     * <p>getEndOffset.</p>
     *
     * @return a int.
     */
    public int getEndOffset() {
        return locator.getEndOffset();
    }

    /**
     * <p>getRegisterCount.</p>
     *
     * @return a int.
     */
    public int getRegisterCount() {
        return locator.getRegisterCount();
    }

    /**
     * <p>bytesToValue.</p>
     *
     * @param data          an array of {@link byte} objects.
     * @param requestOffset a int.
     * @return a {@link Object} object.
     */
    public Object bytesToValue(byte[] data, int requestOffset) {
        try {
            return locator.bytesToValue(data, requestOffset);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Some equipment will not return data lengths that we expect, which causes AIOOBEs. Catch them and convert
            // them into illegal data address exceptions.
            return new ExceptionResult(ExceptionCode.ILLEGAL_DATA_ADDRESS);
        }
    }
}
