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

/**
 * <p>RangeAndOffset class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class RangeAndOffset {
    private int range;
    private int offset;

    /**
     * <p>Constructor for RangeAndOffset.</p>
     *
     * @param range  a int.
     * @param offset a int.
     */
    public RangeAndOffset(int range, int offset) {
        this.range = range;
        this.offset = offset;
    }

    /**
     * This constructor provides a best guess at the function and offset the user wants, with the assumption that the
     * offset will never go over 9999.
     *
     * @param registerId a int.
     */
    public RangeAndOffset(int registerId) {
        if (registerId < 10000) {
            this.range = RegisterRange.COIL_STATUS;
            this.offset = registerId - 1;
        } else if (registerId < 20000) {
            this.range = RegisterRange.INPUT_STATUS;
            this.offset = registerId - 10001;
        } else if (registerId < 40000) {
            this.range = RegisterRange.INPUT_REGISTER;
            this.offset = registerId - 30001;
        } else {
            this.range = RegisterRange.HOLDING_REGISTER;
            this.offset = registerId - 40001;
        }
    }

    /**
     * <p>Getter for the field <code>range</code>.</p>
     *
     * @return a int.
     */
    public int getRange() {
        return range;
    }

    /**
     * <p>Getter for the field <code>offset</code>.</p>
     *
     * @return a int.
     */
    public int getOffset() {
        return offset;
    }
}
