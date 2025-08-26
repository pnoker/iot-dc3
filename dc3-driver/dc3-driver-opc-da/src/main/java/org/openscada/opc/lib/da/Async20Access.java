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
