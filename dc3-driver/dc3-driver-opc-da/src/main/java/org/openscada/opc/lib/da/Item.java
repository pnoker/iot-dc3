/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * OpenSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * OpenSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with OpenSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
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
