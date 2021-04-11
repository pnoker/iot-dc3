/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
