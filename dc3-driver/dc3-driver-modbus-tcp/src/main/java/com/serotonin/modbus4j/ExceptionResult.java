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
package com.serotonin.modbus4j;

import com.serotonin.modbus4j.code.ExceptionCode;

/**
 * <p>ExceptionResult class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ExceptionResult {
    private final byte exceptionCode;
    private final String exceptionMessage;

    /**
     * <p>Constructor for ExceptionResult.</p>
     *
     * @param exceptionCode a byte.
     */
    public ExceptionResult(byte exceptionCode) {
        this.exceptionCode = exceptionCode;
        exceptionMessage = ExceptionCode.getExceptionMessage(exceptionCode);
    }

    /**
     * <p>Getter for the field <code>exceptionCode</code>.</p>
     *
     * @return a byte.
     */
    public byte getExceptionCode() {
        return exceptionCode;
    }

    /**
     * <p>Getter for the field <code>exceptionMessage</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getExceptionMessage() {
        return exceptionMessage;
    }
}
