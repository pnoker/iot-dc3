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
package com.serotonin.modbus4j;

/**
 * <p>ProcessImageListener interface.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
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
