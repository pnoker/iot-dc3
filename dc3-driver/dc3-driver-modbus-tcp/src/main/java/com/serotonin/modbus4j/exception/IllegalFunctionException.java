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
package com.serotonin.modbus4j.exception;

/**
 * <p>IllegalFunctionException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class IllegalFunctionException extends ModbusTransportException {
    private static final long serialVersionUID = -1;

    private final byte functionCode;

    /**
     * <p>Constructor for IllegalFunctionException.</p>
     *
     * @param functionCode a byte.
     * @param slaveId      a int.
     */
    public IllegalFunctionException(byte functionCode, int slaveId) {
        super("Function code: 0x" + Integer.toHexString(functionCode & 0xff), slaveId);
        this.functionCode = functionCode;
    }

    /**
     * <p>Getter for the field <code>functionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getFunctionCode() {
        return functionCode;
    }
}
