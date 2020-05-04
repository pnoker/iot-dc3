/*
 * This file is part of the OpenSCADA project
 * Copyright (C) 2006-2010 TH4 SYSTEMS GmbH (http://th4-systems.com)
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
