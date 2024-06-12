/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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
