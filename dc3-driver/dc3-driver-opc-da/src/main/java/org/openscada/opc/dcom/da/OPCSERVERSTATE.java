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

public enum OPCSERVERSTATE {
    OPC_STATUS_RUNNING(1),
    OPC_STATUS_FAILED(2),
    OPC_STATUS_NOCONFIG(3),
    OPC_STATUS_SUSPENDED(4),
    OPC_STATUS_TEST(5),
    OPC_STATUS_COMM_FAULT(6),
    OPC_STATUS_UNKNOWN(0);

    private int _id;

    private OPCSERVERSTATE(final int id) {
        this._id = id;
    }

    public int id() {
        return this._id;
    }

    public static OPCSERVERSTATE fromID(final int id) {
        switch (id) {
            case 1:
                return OPC_STATUS_RUNNING;
            case 2:
                return OPC_STATUS_FAILED;
            case 3:
                return OPC_STATUS_NOCONFIG;
            case 4:
                return OPC_STATUS_SUSPENDED;
            case 5:
                return OPC_STATUS_TEST;
            case 6:
                return OPC_STATUS_COMM_FAULT;
            default:
                return OPC_STATUS_UNKNOWN;
        }
    }
}
