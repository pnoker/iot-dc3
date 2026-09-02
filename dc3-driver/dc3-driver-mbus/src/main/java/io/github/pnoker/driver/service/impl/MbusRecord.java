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
package io.github.pnoker.driver.service.impl;

import java.util.Arrays;

/**
 * A single decoded M-Bus data record (DIF/VIF header plus raw value bytes).
 *
 * @author pnoker
 * @since 2026.5.22
 */
public class MbusRecord {

    private final int dif;
    private final int vif;
    private final byte[] valueBytes;

    public MbusRecord(int dif, int vif, byte[] valueBytes) {
        this.dif = dif;
        this.vif = vif;
        this.valueBytes = valueBytes;
    }

    /**
     * Return dif.
     *
     * @return get dif result
     */
    public int getDif() {
        return dif;
    }

    /**
     * Return vif.
     *
     * @return get vif result
     */
    public int getVif() {
        return vif;
    }

    /**
     * Return value bytes.
     *
     * @return get value bytes result
     */
    public byte[] getValueBytes() {
        return valueBytes;
    }

    @Override
    public String toString() {
        return "MbusRecord{dif=0x" + Integer.toHexString(dif) + ", vif=0x" + Integer.toHexString(vif) + ", valueBytes="
                + Arrays.toString(valueBytes) + '}';
    }
}
