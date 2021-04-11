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

public enum OPCDATASOURCE {
    OPC_DS_CACHE(1),
    OPC_DS_DEVICE(2),
    OPC_DS_UNKNOWN(0);

    private int _id;

    private OPCDATASOURCE(final int id) {
        this._id = id;
    }

    public int id() {
        return this._id;
    }

    public static OPCDATASOURCE fromID(final int id) {
        switch (id) {
            case 1:
                return OPC_DS_CACHE;
            case 2:
                return OPC_DS_DEVICE;
            default:
                return OPC_DS_UNKNOWN;
        }
    }
}
