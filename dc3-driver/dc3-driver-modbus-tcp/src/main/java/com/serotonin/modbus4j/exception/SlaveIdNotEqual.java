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
package com.serotonin.modbus4j.exception;

public class SlaveIdNotEqual extends ModbusTransportException {
    private static final long serialVersionUID = -1;

    /**
     * Exception to show that the requested slave id is not what was received
     *
     * @param requestSlaveId  - slave id requested
     * @param responseSlaveId - slave id of response
     */
    public SlaveIdNotEqual(int requestSlaveId, int responseSlaveId) {
        super("Response slave id different from requested id", requestSlaveId);
    }
}
