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

import org.jinterop.dcom.core.JIVariant;

/**
 * Data for a write request to the server
 *
 * @author Jens Reimann <jens.reimann@th4-systems.com>
 */
public class WriteRequest {
    private int serverHandle = 0;

    private JIVariant value = JIVariant.EMPTY();

    public WriteRequest() {
    }

    public WriteRequest(final WriteRequest request) {
        this.serverHandle = request.serverHandle;
        this.value = request.value;
    }

    /**
     * Create a new write request with pre-fille data
     *
     * @param serverHandle the server handle of the item to write to
     * @param value        the value to write.
     */
    public WriteRequest(final int serverHandle, final JIVariant value) {
        this.serverHandle = serverHandle;
        this.value = value;
    }

    public int getServerHandle() {
        return this.serverHandle;
    }

    public void setServerHandle(final int serverHandle) {
        this.serverHandle = serverHandle;
    }

    public JIVariant getValue() {
        return this.value;
    }

    public void setValue(final JIVariant value) {
        this.value = value;
    }
}
