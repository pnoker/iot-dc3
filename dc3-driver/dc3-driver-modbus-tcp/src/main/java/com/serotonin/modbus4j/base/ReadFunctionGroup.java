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

import com.serotonin.modbus4j.code.RegisterRange;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>ReadFunctionGroup class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class ReadFunctionGroup<K> {
    private final SlaveAndRange slaveAndRange;
    private final int functionCode;
    private final List<KeyedModbusLocator<K>> locators = new ArrayList<>();
    private int startOffset = 65536;
    private int length = 0;

    /**
     * <p>Constructor for ReadFunctionGroup.</p>
     *
     * @param locator a {@link KeyedModbusLocator} object.
     */
    public ReadFunctionGroup(KeyedModbusLocator<K> locator) {
        slaveAndRange = locator.getSlaveAndRange();
        functionCode = RegisterRange.getReadFunctionCode(slaveAndRange.getRange());
        add(locator);
    }

    /**
     * <p>add.</p>
     *
     * @param locator a {@link KeyedModbusLocator} object.
     */
    public void add(KeyedModbusLocator<K> locator) {
        if (startOffset > locator.getOffset())
            startOffset = locator.getOffset();
        if (length < locator.getEndOffset() - startOffset + 1)
            length = locator.getEndOffset() - startOffset + 1;
        locators.add(locator);
    }

    /**
     * <p>Getter for the field <code>startOffset</code>.</p>
     *
     * @return a int.
     */
    public int getStartOffset() {
        return startOffset;
    }

    /**
     * <p>getEndOffset.</p>
     *
     * @return a int.
     */
    public int getEndOffset() {
        return startOffset + length - 1;
    }

    /**
     * <p>Getter for the field <code>slaveAndRange</code>.</p>
     *
     * @return a {@link SlaveAndRange} object.
     */
    public SlaveAndRange getSlaveAndRange() {
        return slaveAndRange;
    }

    /**
     * <p>Getter for the field <code>length</code>.</p>
     *
     * @return a int.
     */
    public int getLength() {
        return length;
    }

    /**
     * <p>Getter for the field <code>functionCode</code>.</p>
     *
     * @return a int.
     */
    public int getFunctionCode() {
        return functionCode;
    }

    /**
     * <p>Getter for the field <code>locators</code>.</p>
     *
     * @return a {@link List} object.
     */
    public List<KeyedModbusLocator<K>> getLocators() {
        return locators;
    }
}
