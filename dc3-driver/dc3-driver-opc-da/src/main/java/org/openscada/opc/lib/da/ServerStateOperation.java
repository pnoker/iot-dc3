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
import org.openscada.opc.dcom.da.impl.OPCServer;

/**
 * A server state operation which can be interruped
 *
 * @author Jens Reimann
 */
@Slf4j
public class ServerStateOperation implements Runnable {

    public OPCSERVERSTATUS _serverStatus = null;

    public OPCServer _server;

    public Throwable _error;

    public Object _lock = new Object();

    public boolean _running = false;

    public ServerStateOperation(final OPCServer server) {
        super();
        this._server = server;
    }

    /**
     * Perform the operation.
     * <p>
     * This method will block until either the serve state has been aquired or the
     * timeout triggers cancels the call.
     * </p>
     */
    public void run() {
        synchronized (this._lock) {
            this._running = true;
        }
        try {
            this._serverStatus = this._server.getStatus();
            synchronized (this._lock) {
                this._running = false;
                this._lock.notify();
            }
        } catch (Throwable e) {
            log.info("Failed to get server state", e);
            this._error = e;
            this._running = false;
            synchronized (this._lock) {
                this._lock.notify();
            }
        }

    }

    /**
     * Get the server state with a timeout.
     *
     * @param timeout the timeout in ms
     * @return the server state or <code>null</code> if the server is not set.
     * @throws Throwable any error that occurred
     */
    public OPCSERVERSTATUS getServerState(final int timeout) throws Throwable {
        if (this._server == null) {
            log.debug("No connection to server. Skipping...");
            return null;
        }

        Thread t = new Thread(this, "OPCServerStateReader");

        synchronized (this._lock) {
            t.start();
            this._lock.wait(timeout);
            if (this._running) {
                log.warn("State operation still running. Interrupting...");
                t.interrupt();
                throw new InterruptedException("Interrupted getting server state");
            }
        }
        if (this._error != null) {
            log.warn("An error occurred while getting server state", this._error);
            throw this._error;
        }

        return this._serverStatus;
    }

}
