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

package org.openscada.opc.lib.da;

import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIVariant;

@Slf4j
public class Item {

    private Group _group = null;

    private int _serverHandle = 0;

    private int _clientHandle = 0;

    private String _id = null;

    Item(final Group group, final int serverHandle, final int clientHandle, final String id) {
        super();
        log.debug(String.format("Adding new item '%s' (0x%08X) for group %s", id, serverHandle, group.toString()));
        this._group = group;
        this._serverHandle = serverHandle;
        this._clientHandle = clientHandle;
        this._id = id;
    }

    public Group getGroup() {
        return this._group;
    }

    public int getServerHandle() {
        return this._serverHandle;
    }

    public int getClientHandle() {
        return this._clientHandle;
    }

    public String getId() {
        return this._id;
    }

    public void setActive(final boolean state) throws JIException {
        this._group.setActive(state, this);
    }

    public ItemState read(final boolean device) throws JIException {
        return this._group.read(device, this).get(this);
    }

    public Integer write(final JIVariant value) throws JIException {
        return this._group.write(new WriteRequest[]{new WriteRequest(this, value)}).get(this);
    }
}
