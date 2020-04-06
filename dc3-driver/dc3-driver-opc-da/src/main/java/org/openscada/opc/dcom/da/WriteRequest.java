/*
 * This file is part of the OpenSCADA project
 *
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
 * Copyright (C) 2013 Jens Reimann (ctron@dentrassi.de)
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
