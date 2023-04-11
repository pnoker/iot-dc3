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

/**
 * Class for maintaining the profile of a slave device on the master side. Initially, we assume that the device is fully
 * featured, and then we note function failures so that we know how requests should subsequently be sent.
 *
 * @author mlohbihler
 * @version 5.0.0
 */
public class SlaveProfile {
    private boolean writeMaskRegister = true;

    /**
     * <p>Setter for the field <code>writeMaskRegister</code>.</p>
     *
     * @param writeMaskRegister a boolean.
     */
    public void setWriteMaskRegister(boolean writeMaskRegister) {
        this.writeMaskRegister = writeMaskRegister;
    }

    /**
     * <p>Getter for the field <code>writeMaskRegister</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getWriteMaskRegister() {
        return writeMaskRegister;
    }
}
