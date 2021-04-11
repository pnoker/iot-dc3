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

    public int id() {
        return this._id;
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
}
