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
