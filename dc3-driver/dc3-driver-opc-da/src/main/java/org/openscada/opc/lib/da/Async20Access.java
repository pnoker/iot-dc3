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
import org.openscada.opc.dcom.common.EventHandler;
import org.openscada.opc.dcom.common.KeyedResult;
import org.openscada.opc.dcom.common.KeyedResultSet;
import org.openscada.opc.dcom.common.ResultSet;
import org.openscada.opc.dcom.da.IOPCDataCallback;
import org.openscada.opc.dcom.da.OPCDATASOURCE;
import org.openscada.opc.dcom.da.ValueData;
import org.openscada.opc.dcom.da.impl.OPCAsyncIO2;
import org.openscada.opc.lib.common.NotConnectedException;

import java.net.UnknownHostException;

@Slf4j
public class Async20Access extends AccessBase implements IOPCDataCallback {

    private EventHandler eventHandler = null;

    private boolean initialRefresh = false;

    public Async20Access(final Server server, final int period, final boolean initialRefresh) throws IllegalArgumentException, UnknownHostException, NotConnectedException, JIException, DuplicateGroupException {
        super(server, period);
        this.initialRefresh = initialRefresh;
    }

    public Async20Access(final Server server, final int period, final boolean initialRefresh, final String logTag) throws IllegalArgumentException, UnknownHostException, NotConnectedException, JIException, DuplicateGroupException {
        super(server, period, logTag);
        this.initialRefresh = initialRefresh;
    }

    @Override
    protected synchronized void start() throws JIException, IllegalArgumentException, UnknownHostException, NotConnectedException, DuplicateGroupException {
        if (isActive()) {
            return;
        }

        super.start();

        this.eventHandler = this.group.attach(this);
        if (!this.items.isEmpty() && this.initialRefresh) {
            final OPCAsyncIO2 async20 = this.group.getAsyncIO20();
            if (async20 == null) {
                throw new NotConnectedException();
            }

            this.group.getAsyncIO20().refresh(OPCDATASOURCE.OPC_DS_CACHE, 0);
        }
    }

    @Override
    protected synchronized void stop() throws JIException {
        if (!isActive()) {
            return;
        }

        if (this.eventHandler != null) {
            try {
                this.eventHandler.detach();
            } catch (final Throwable e) {
                log.warn("Failed to detach group", e);
            }

            this.eventHandler = null;
        }

        super.stop();
    }

    public void cancelComplete(final int transactionId, final int serverGroupHandle) {
    }

    public void dataChange(final int transactionId, final int serverGroupHandle, final int masterQuality, final int masterErrorCode, final KeyedResultSet<Integer, ValueData> result) {
        log.debug("dataChange - transId {}, items: {}", transactionId, result.size());

        final Group group = this.group;
        if (group == null) {
            return;
        }

        for (final KeyedResult<Integer, ValueData> entry : result) {
            final Item item = group.findItemByClientHandle(entry.getKey());
            log.debug("Update for '{}'", item.getId());
            updateItem(item, new ItemState(entry.getErrorCode(), entry.getValue().getValue(), entry.getValue().getTimestamp(), entry.getValue().getQuality()));
        }
    }

    public void readComplete(final int transactionId, final int serverGroupHandle, final int masterQuality, final int masterErrorCode, final KeyedResultSet<Integer, ValueData> result) {
        log.debug("readComplete - transId {}", transactionId);
    }

    public void writeComplete(final int transactionId, final int serverGroupHandle, final int masterErrorCode, final ResultSet<Integer> result) {
        log.debug("writeComplete - transId {}", transactionId);
    }
}
