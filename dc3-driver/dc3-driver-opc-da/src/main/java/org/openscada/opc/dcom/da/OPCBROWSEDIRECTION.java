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

public enum OPCBROWSEDIRECTION {
    OPC_BROWSE_UP(1),
    OPC_BROWSE_DOWN(2),
    OPC_BROWSE_TO(3),
    OPC_BROWSE_UNKNOWN(0);

    private int _id;

    private OPCBROWSEDIRECTION(final int id) {
        this._id = id;
    }

    public static OPCBROWSEDIRECTION fromID(final int id) {
        switch (id) {
            case 1:
                return OPC_BROWSE_UP;
            case 2:
                return OPC_BROWSE_DOWN;
            case 3:
                return OPC_BROWSE_TO;
            default:
                return OPC_BROWSE_UNKNOWN;
        }
    }

    public int id() {
        return this._id;
    }
}
