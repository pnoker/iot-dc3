/*
Copyright 2016 S7connector members (github.com/s7connector)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.github.s7connector.api;

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
    SYSINFO(3), // local of caller
    SYSTEMFLAGS(5), // S7 counters
    TIMER(29), // S7 timers
    TIMER200(31), // IEC counters (200 family)
    V(0x87); // IEC timers (200 family)

    /**
     * Function Code
     */
    int code;

    /**
     * Constructor
     */
    DaveArea(final int code) {
        this.code = code;
    }

    /**
     * Returns the function code as associated
     */
    public int getCode() {
        return this.code;
    }
}
