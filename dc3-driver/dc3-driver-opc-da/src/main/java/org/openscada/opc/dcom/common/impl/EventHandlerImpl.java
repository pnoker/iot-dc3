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

package org.openscada.opc.dcom.common.impl;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.IJIComObject;
import org.jinterop.dcom.core.JIFrameworkHelper;
import org.openscada.opc.dcom.common.EventHandler;

public class EventHandlerImpl implements EventHandler {
    private String identifier = null;

    private IJIComObject object = null;

    public String getIdentifier() {
        return this.identifier;
    }

    public synchronized IJIComObject getObject() {
        return this.object;
    }

    public synchronized void setInfo(final IJIComObject object, final String identifier) {
        this.object = object;
        this.identifier = identifier;
    }

    public synchronized void detach() throws JIException {
        if (this.object != null && this.identifier != null) {
            try {
                JIFrameworkHelper.detachEventHandler(this.object, this.identifier);
            } finally {
                this.object = null;
                this.identifier = null;
            }
        }
    }

}
