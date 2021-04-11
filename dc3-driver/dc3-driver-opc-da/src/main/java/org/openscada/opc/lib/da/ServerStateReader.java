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
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerStateReader {

    private Server _server = null;

    private ScheduledExecutorService _scheduler = null;

    private final List<ServerStateListener> _listeners = new CopyOnWriteArrayList<ServerStateListener>();

    private ScheduledFuture<?> _job = null;

    public ServerStateReader(final Server server) {
        super();
        this._server = server;
        this._scheduler = this._server.getScheduler();
    }

    /**
     * Create a new server state reader. Please note that the scheduler might get
     * blocked for a short period of time in case of a connection failure!
     *
     * @param server    the server to check
     * @param scheduler the scheduler to use
     */
    public ServerStateReader(final Server server, final ScheduledExecutorService scheduler) {
        super();
        this._server = server;
        this._scheduler = scheduler;
    }

    public synchronized void start() {
        if (this._job != null) {
            return;
        }

        this._job = this._scheduler.scheduleAtFixedRate(new Runnable() {

            public void run() {
                once();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        this._job.cancel(false);
        this._job = null;
    }

    protected void once() {
        log.debug("Reading server state");

        final OPCSERVERSTATUS state = this._server.getServerState();

        for (final ServerStateListener listener : new ArrayList<ServerStateListener>(this._listeners)) {
            listener.stateUpdate(state);
        }
    }

    public void addListener(final ServerStateListener listener) {
        this._listeners.add(listener);
    }

    public void removeListener(final ServerStateListener listener) {
        this._listeners.remove(listener);
    }
}
