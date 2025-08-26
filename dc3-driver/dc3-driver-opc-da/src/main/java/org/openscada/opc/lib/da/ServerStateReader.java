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
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerStateReader {

    private final List<ServerStateListener> _listeners = new CopyOnWriteArrayList<ServerStateListener>();
    private Server _server = null;
    private ScheduledExecutorService _scheduler = null;
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
