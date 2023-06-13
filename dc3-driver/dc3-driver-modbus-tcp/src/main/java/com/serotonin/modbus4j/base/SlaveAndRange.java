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
 * <p>SlaveAndRange class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + range;
        result = prime * result + slaveId;
        return result;
    }

    /**
     * {@inheritDoc}
     */
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
