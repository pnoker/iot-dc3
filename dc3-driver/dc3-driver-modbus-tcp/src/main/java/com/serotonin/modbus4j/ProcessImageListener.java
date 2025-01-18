/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

/**
 * <p>ProcessImageListener interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 2024.3.10
 */
public interface ProcessImageListener {
    /**
     * <p>coilWrite.</p>
     *
     * @param offset   a int.
     * @param oldValue a boolean.
     * @param newValue a boolean.
     */
    public void coilWrite(int offset, boolean oldValue, boolean newValue);

    /**
     * <p>holdingRegisterWrite.</p>
     *
     * @param offset   a int.
     * @param oldValue a short.
     * @param newValue a short.
     */
    public void holdingRegisterWrite(int offset, short oldValue, short newValue);
}
