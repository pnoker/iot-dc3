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
package com.serotonin.modbus4j.base;

/**
 * <p>SlaveAndRange class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class SlaveAndRange {
    private final int slaveId;
    private final int range;

    /**
     * <p>Constructor for SlaveAndRange.</p>
     *
     * @param slaveId a int.
     * @param range   a int.
     */
    public SlaveAndRange(int slaveId, int range) {
        ModbusUtils.validateSlaveId(slaveId, true);

        this.slaveId = slaveId;
        this.range = range;
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
     * <p>Getter for the field <code>slaveId</code>.</p>
     *
     * @return a int.
     */
    public int getSlaveId() {
        return slaveId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + range;
        result = prime * result + slaveId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final SlaveAndRange other = (SlaveAndRange) obj;
        if (range != other.range)
            return false;
        if (slaveId != other.slaveId)
            return false;
        return true;
    }
}
