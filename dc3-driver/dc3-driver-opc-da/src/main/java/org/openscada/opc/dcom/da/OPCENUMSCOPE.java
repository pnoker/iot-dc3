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

public enum OPCENUMSCOPE {
    OPC_ENUM_PRIVATE_CONNECTIONS(1),
    OPC_ENUM_PUBLIC_CONNECTIONS(2),
    OPC_ENUM_ALL_CONNECTIONS(3),
    OPC_ENUM_PRIVATE(4),
    OPC_ENUM_PUBLIC(5),
    OPC_ENUM_ALL(6),
    OPC_ENUM_UNKNOWN(0);

    private int _id;

    private OPCENUMSCOPE(final int id) {
        this._id = id;
    }

    public static OPCENUMSCOPE fromID(final int id) {
        switch (id) {
            case 1:
                return OPC_ENUM_PRIVATE_CONNECTIONS;
            case 2:
                return OPC_ENUM_PUBLIC_CONNECTIONS;
            case 3:
                return OPC_ENUM_ALL_CONNECTIONS;
            case 4:
                return OPC_ENUM_PRIVATE;
            case 5:
                return OPC_ENUM_PUBLIC;
            case 6:
                return OPC_ENUM_ALL;
            default:
                return OPC_ENUM_UNKNOWN;
        }
    }

    public int id() {
        return this._id;
    }
}
