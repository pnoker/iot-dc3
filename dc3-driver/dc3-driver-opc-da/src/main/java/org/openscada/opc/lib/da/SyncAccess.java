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
import org.openscada.opc.lib.common.NotConnectedException;

import java.net.UnknownHostException;
import java.util.Map;

@Slf4j
public class SyncAccess extends AccessBase implements Runnable {

    private Thread runner = null;

    private Throwable lastError = null;

    public SyncAccess(final Server server, final int period) throws IllegalArgumentException, UnknownHostException, NotConnectedException, JIException, DuplicateGroupException {
        super(server, period);
    }

    public SyncAccess(final Server server, final int period, final String logTag) throws IllegalArgumentException, UnknownHostException, NotConnectedException, JIException, DuplicateGroupException {
        super(server, period, logTag);
    }

    public void run() {
        while (this.active) {
            try {
                runOnce();
                if (this.lastError != null) {
                    this.lastError = null;
                    handleError(null);
                }
            } catch (Throwable e) {
                log.error("Sync read failed", e);
                handleError(e);
                this.server.disconnect();
            }

            try {
                Thread.sleep(getPeriod());
            } catch (InterruptedException e) {
            }
        }
    }

    protected void runOnce() throws JIException {
        if (!this.active || this.group == null) {
            return;
        }

        Map<Item, ItemState> result;

        // lock only this section since we could get into a deadlock otherwise
        // calling updateItem
        synchronized (this) {
            Item[] items = this.items.keySet().toArray(new Item[this.items.size()]);
            result = this.group.read(false, items);
        }

        for (Map.Entry<Item, ItemState> entry : result.entrySet()) {
            updateItem(entry.getKey(), entry.getValue());
        }

    }

    @Override
    protected synchronized void start() throws JIException, IllegalArgumentException, UnknownHostException, NotConnectedException, DuplicateGroupException {
        super.start();

        this.runner = new Thread(this, "UtgardSyncReader");
        this.runner.setDaemon(true);
        this.runner.start();
    }

    @Override
    protected synchronized void stop() throws JIException {
        super.stop();

        this.runner = null;
        this.items.clear();
    }
}
