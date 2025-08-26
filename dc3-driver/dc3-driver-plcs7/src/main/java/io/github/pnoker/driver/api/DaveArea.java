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
package io.github.pnoker.driver.api;

/**
 * @author Thomas Rudin
 */
public enum DaveArea {
    ANALOGINPUTS200(6), // System info of 200 family
    ANALOGOUTPUTS200(7), // System flags of 200 family
    COUNTER(28), // analog inputs of 200 family
    COUNTER200(30), // analog outputs of 200 family
    DB(0x84), // Peripheral I/O
    DI(0x85),
    FLAGS(0x83),
    INPUTS(0x81),
    LOCAL(0x86), // data blocks
    OUTPUTS(0x82), // instance data blocks
    P(0x80), // not tested
    SYSTEM_INFO(3), // local of caller
    SYSTEM_FLAGS(5), // S7 counters
    TIMER(29), // S7 timers
    TIMER200(31), // IEC counters (200 family)
    V(0x87); // IEC timers (200 family)

    /**
     * Function Code
     */
    final int code;

    DaveArea(final int code) {
        this.code = code;
    }

    /**
     * Returns the function code as associated
     *
     * @return code
     */
    public int getCode() {
        return this.code;
    }
}
