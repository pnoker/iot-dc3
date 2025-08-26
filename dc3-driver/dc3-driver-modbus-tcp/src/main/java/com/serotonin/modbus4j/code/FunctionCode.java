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
package com.serotonin.modbus4j.code;

/**
 * <p>FunctionCode class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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
