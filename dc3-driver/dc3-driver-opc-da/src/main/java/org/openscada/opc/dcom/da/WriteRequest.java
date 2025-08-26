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

import org.jinterop.dcom.core.JIVariant;

/**
 * Data for a write request to the server
 *
 * @author Jens Reimann jens.reimann@th4-systems.com
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
