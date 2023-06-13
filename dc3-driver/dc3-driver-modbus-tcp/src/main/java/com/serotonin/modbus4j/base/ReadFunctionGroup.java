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
package com.serotonin.modbus4j.base;

import com.serotonin.modbus4j.code.RegisterRange;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>ReadFunctionGroup class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
