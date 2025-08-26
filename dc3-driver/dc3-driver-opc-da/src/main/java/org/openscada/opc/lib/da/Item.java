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
