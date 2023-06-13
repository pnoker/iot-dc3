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
package com.serotonin.modbus4j.code;

/**
 * <p>ExceptionCode class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class ExceptionCode {
    /**
     * Constant <code>ILLEGAL_FUNCTION=0x1</code>
     */
    public static final byte ILLEGAL_FUNCTION = 0x1;
    /**
     * Constant <code>ILLEGAL_DATA_ADDRESS=0x2</code>
     */
    public static final byte ILLEGAL_DATA_ADDRESS = 0x2;
    /**
     * Constant <code>ILLEGAL_DATA_VALUE=0x3</code>
     */
    public static final byte ILLEGAL_DATA_VALUE = 0x3;
    /**
     * Constant <code>SLAVE_DEVICE_FAILURE=0x4</code>
     */
    public static final byte SLAVE_DEVICE_FAILURE = 0x4;
    /**
     * Constant <code>ACKNOWLEDGE=0x5</code>
     */
    public static final byte ACKNOWLEDGE = 0x5;
    /**
     * Constant <code>SLAVE_DEVICE_BUSY=0x6</code>
     */
    public static final byte SLAVE_DEVICE_BUSY = 0x6;
    /**
     * Constant <code>MEMORY_PARITY_ERROR=0x8</code>
     */
    public static final byte MEMORY_PARITY_ERROR = 0x8;
    /**
     * Constant <code>GATEWAY_PATH_UNAVAILABLE=0xa</code>
     */
    public static final byte GATEWAY_PATH_UNAVAILABLE = 0xa;
    /**
     * Constant <code>GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND=0xb</code>
     */
    public static final byte GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND = 0xb;

    /**
     * <p>getExceptionMessage.</p>
     *
     * @param id a byte.
     * @return a {@link String} object.
     */
    public static String getExceptionMessage(byte id) {
        switch (id) {
            case ILLEGAL_FUNCTION:
                return "Illegal function";
            case ILLEGAL_DATA_ADDRESS:
                return "Illegal data address";
            case ILLEGAL_DATA_VALUE:
                return "Illegal data value";
            case SLAVE_DEVICE_FAILURE:
                return "Slave device failure";
            case ACKNOWLEDGE:
                return "Acknowledge";
            case SLAVE_DEVICE_BUSY:
                return "Slave device busy";
            case MEMORY_PARITY_ERROR:
                return "Memory parity error";
            case GATEWAY_PATH_UNAVAILABLE:
                return "Gateway path unavailable";
            case GATEWAY_TARGET_DEVICE_FAILED_TO_RESPOND:
                return "Gateway target device failed to respond";
        }
        return "Unknown exception code: " + id;
    }
}
