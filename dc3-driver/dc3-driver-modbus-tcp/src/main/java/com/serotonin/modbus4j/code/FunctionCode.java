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
 * <p>FunctionCode class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class FunctionCode {
    /**
     * Constant <code>READ_COILS=1</code>
     */
    public static final byte READ_COILS = 1;
    /**
     * Constant <code>READ_DISCRETE_INPUTS=2</code>
     */
    public static final byte READ_DISCRETE_INPUTS = 2;
    /**
     * Constant <code>READ_HOLDING_REGISTERS=3</code>
     */
    public static final byte READ_HOLDING_REGISTERS = 3;
    /**
     * Constant <code>READ_INPUT_REGISTERS=4</code>
     */
    public static final byte READ_INPUT_REGISTERS = 4;
    /**
     * Constant <code>WRITE_COIL=5</code>
     */
    public static final byte WRITE_COIL = 5;
    /**
     * Constant <code>WRITE_REGISTER=6</code>
     */
    public static final byte WRITE_REGISTER = 6;
    /**
     * Constant <code>READ_EXCEPTION_STATUS=7</code>
     */
    public static final byte READ_EXCEPTION_STATUS = 7;
    /**
     * Constant <code>WRITE_COILS=15</code>
     */
    public static final byte WRITE_COILS = 15;
    /**
     * Constant <code>WRITE_REGISTERS=16</code>
     */
    public static final byte WRITE_REGISTERS = 16;
    /**
     * Constant <code>REPORT_SLAVE_ID=17</code>
     */
    public static final byte REPORT_SLAVE_ID = 17;
    /**
     * Constant <code>WRITE_MASK_REGISTER=22</code>
     */
    public static final byte WRITE_MASK_REGISTER = 22;

    /**
     * <p>toString.</p>
     *
     * @param code a byte.
     * @return a {@link String} object.
     */
    public static String toString(byte code) {
        return Integer.toString(code & 0xff);
    }
}
