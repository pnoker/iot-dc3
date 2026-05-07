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
 * Enum representing different memory areas in Siemens S7 PLCs. Each enum value
 * corresponds to a specific memory area with its associated code. These areas are used
 * for reading from and writing to different types of memory in the PLC, such as data
 * blocks (DB), inputs (I), outputs (Q), etc.
 *
 * @author Thomas Rudin
 * @version 2025.9.0
 * @since 2022.1.0
 */
public enum DaveArea {

    ANALOGINPUTS200(6), // Analog inputs (S7-200 family)
    ANALOGOUTPUTS200(7), // Analog outputs (S7-200 family)
    COUNTER(28), // S7 counters
    COUNTER200(30), // IEC counters (S7-200 family)
    DB(0x84), // Data blocks
    DI(0x85), // Instance data blocks
    FLAGS(0x83), // Merker/flag memory (M area)
    INPUTS(0x81), // Process image inputs (I area)
    LOCAL(0x86), // Local data (L stack)
    OUTPUTS(0x82), // Process image outputs (Q area)
    P(0x80), // Peripheral I/O (direct access)
    SYSTEM_INFO(3), // System information
    SYSTEM_FLAGS(5), // System flags
    TIMER(29), // S7 timers
    TIMER200(31), // IEC timers (S7-200 family)
    V(0x87); // Variable memory (S7-200 V area)

    /**
     * S7 area function code.
     */
    final int code;

    DaveArea(final int code) {
        this.code = code;
    }

    /**
     * @return the S7 area code
     */
    public int getCode() {
        return this.code;
    }

}
