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
