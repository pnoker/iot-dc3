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
