/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
 * <p>IllegalDataTypeException class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2024.3.10
 */
public class IllegalDataTypeException extends ModbusIdException {
    private static final long serialVersionUID = -1;

    /**
     * <p>Constructor for IllegalDataTypeException.</p>
     *
     * @param message a {@link String} object.
     */
    public IllegalDataTypeException(String message) {
        super(message);
    }
}
