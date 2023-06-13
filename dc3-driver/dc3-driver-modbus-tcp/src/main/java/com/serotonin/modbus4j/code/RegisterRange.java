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
 * <p>RegisterRange class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class RegisterRange {
    /**
     * Constant <code>COIL_STATUS=1</code>
     */
    public static final int COIL_STATUS = 1;
    /**
     * Constant <code>INPUT_STATUS=2</code>
     */
    public static final int INPUT_STATUS = 2;
    /**
     * Constant <code>HOLDING_REGISTER=3</code>
     */
    public static final int HOLDING_REGISTER = 3;
    /**
     * Constant <code>INPUT_REGISTER=4</code>
     */
    public static final int INPUT_REGISTER = 4;

    /**
     * <p>getFrom.</p>
     *
     * @param id a int.
     * @return a int.
     */
    public static int getFrom(int id) {
        switch (id) {
            case COIL_STATUS:
                return 0;
            case INPUT_STATUS:
                return 0x10000;
            case HOLDING_REGISTER:
                return 0x40000;
            case INPUT_REGISTER:
                return 0x30000;
        }
        return -1;
    }

    /**
     * <p>getTo.</p>
     *
     * @param id a int.
     * @return a int.
     */
    public static int getTo(int id) {
        switch (id) {
            case COIL_STATUS:
                return 0xffff;
            case INPUT_STATUS:
                return 0x1ffff;
            case HOLDING_REGISTER:
                return 0x4ffff;
            case INPUT_REGISTER:
                return 0x3ffff;
        }
        return -1;
    }

    /**
     * <p>getReadFunctionCode.</p>
     *
     * @param id a int.
     * @return a int.
     */
    public static int getReadFunctionCode(int id) {
        switch (id) {
            case COIL_STATUS:
                return FunctionCode.READ_COILS;
            case INPUT_STATUS:
                return FunctionCode.READ_DISCRETE_INPUTS;
            case HOLDING_REGISTER:
                return FunctionCode.READ_HOLDING_REGISTERS;
            case INPUT_REGISTER:
                return FunctionCode.READ_INPUT_REGISTERS;
        }
        return -1;
    }
}
