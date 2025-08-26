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

package org.openscada.opc.dcom.da;

public class OPCGroupState {
    private int _updateRate = 1000;

    private boolean _active = true;

    private String _name = "";

    private int _timeBias = 0;

    private float _percentDeadband = 0.0f;

    private int _localeID = 0;

    private int _clientHandle = 0;

    private int _serverHandle = 0;

    public boolean isActive() {
        return this._active;
    }

    public void setActive(final boolean active) {
        this._active = active;
    }

    public int getClientHandle() {
        return this._clientHandle;
    }

    public void setClientHandle(final int clientHandle) {
        this._clientHandle = clientHandle;
    }

    public int getLocaleID() {
        return this._localeID;
    }

    public void setLocaleID(final int localeID) {
        this._localeID = localeID;
    }

    public String getName() {
        return this._name;
    }

    public void setName(final String name) {
        this._name = name;
    }

    public float getPercentDeadband() {
        return this._percentDeadband;
    }

    public void setPercentDeadband(final float percentDeadband) {
        this._percentDeadband = percentDeadband;
    }

    public int getServerHandle() {
        return this._serverHandle;
    }

    public void setServerHandle(final int serverHandle) {
        this._serverHandle = serverHandle;
    }

    public int getTimeBias() {
        return this._timeBias;
    }

    public void setTimeBias(final int timeBias) {
        this._timeBias = timeBias;
    }

    public int getUpdateRate() {
        return this._updateRate;
    }

    public void setUpdateRate(final int updateRate) {
        this._updateRate = updateRate;
    }
}
