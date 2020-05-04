/*
 * This file is part of the openSCADA project
 * Copyright (C) 2006-2011 TH4 SYSTEMS GmbH (http://th4-systems.com)
 *
 * openSCADA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 3
 * only, as published by the Free Software Foundation.
 *
 * openSCADA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License version 3 for more details
 * (a copy is included in the LICENSE file that accompanied this code).
 *
 * You should have received a copy of the GNU Lesser General Public License
 * version 3 along with openSCADA. If not, see
 * <http://opensource.org/licenses/lgpl-3.0.html> for a copy of the LGPLv3 License.
 */

package org.openscada.opc.lib.da;

import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JIProgId;
import org.jinterop.dcom.core.JISession;
import org.openscada.opc.dcom.da.OPCNAMESPACETYPE;
import org.openscada.opc.dcom.da.OPCSERVERSTATUS;
import org.openscada.opc.dcom.da.impl.OPCBrowseServerAddressSpace;
import org.openscada.opc.dcom.da.impl.OPCGroupStateMgt;
import org.openscada.opc.dcom.da.impl.OPCServer;
import org.openscada.opc.lib.common.AlreadyConnectedException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.common.NotConnectedException;
import org.openscada.opc.lib.da.browser.FlatBrowser;
import org.openscada.opc.lib.da.browser.TreeBrowser;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

@Slf4j
public class Server {

    private final ConnectionInformation connectionInformation;

    private JISession session;

    private JIComServer comServer;

    private OPCServer server;

    private boolean defaultActive = true;

    private int defaultUpdateRate = 1000;

    private Integer defaultTimeBias;

    private Float defaultPercentDeadband;

    private int defaultLocaleID = 0;

    private ErrorMessageResolver errorMessageResolver;

    private final Map<Integer, Group> groups = new HashMap<>();

    private final List<ServerConnectionStateListener> stateListeners = new CopyOnWriteArrayList<>();

    private final ScheduledExecutorService scheduler;

    public Server(final ConnectionInformation connectionInformation,
                  final ScheduledExecutorService scheduler) {
        super();
        this.connectionInformation = connectionInformation;
        this.scheduler = scheduler;
    }

    /**
     * Gets the scheduler for the server. Note that this scheduler might get
     * blocked for a short time if the connection breaks. It should not be used
     * for time critical operations.
     *
     * @return the scheduler for the server
     */
    public ScheduledExecutorService getScheduler() {
        return this.scheduler;
    }

    protected synchronized boolean isConnected() {
        return this.session != null;
    }

    public synchronized void connect() throws IllegalArgumentException, UnknownHostException, JIException, AlreadyConnectedException {
        if (isConnected()) {
            throw new AlreadyConnectedException();
        }

        final int socketTimeout = Integer.getInteger("rpc.socketTimeout", 0);
        log.debug(String.format("Socket timeout: %s ", socketTimeout));

        try {
            if (this.connectionInformation.getClsid() != null) {
                this.session = JISession.createSession(
                        this.connectionInformation.getDomain(),
                        this.connectionInformation.getUser(),
                        this.connectionInformation.getPassword());
                this.session.setGlobalSocketTimeout(socketTimeout);
                this.session.useSessionSecurity(true);
                this.comServer = new JIComServer(
                        JIClsid.valueOf(this.connectionInformation.getClsid()),
                        this.connectionInformation.getHost(), this.session);
            } else if (this.connectionInformation.getProgId() != null) {
                this.session = JISession.createSession(
                        this.connectionInformation.getDomain(),
                        this.connectionInformation.getUser(),
                        this.connectionInformation.getPassword());
                this.session.setGlobalSocketTimeout(socketTimeout);
                this.comServer = new JIComServer(
                        JIProgId.valueOf(this.connectionInformation.getProgId()),
                        this.connectionInformation.getHost(), this.session);
            } else {
                throw new IllegalArgumentException("Neither clsid nor progid is valid!");
            }

            this.server = new OPCServer(this.comServer.createInstance());
            this.errorMessageResolver = new ErrorMessageResolver(
                    this.server.getCommon(), this.defaultLocaleID);
        } catch (final UnknownHostException e) {
            log.error("Unknown host when connecting to server", e);
            cleanup();
            throw e;
        } catch (final JIException e) {
            log.error("Failed to connect to server", e);
            cleanup();
            throw e;
        } catch (final Throwable e) {
            log.error("Unknown error", e);
            cleanup();
            throw new RuntimeException(e);
        }

        notifyConnectionStateChange(true);
    }

    /**
     * cleanup after the connection is closed
     */
    protected void cleanup() {
        log.debug("Destroying DCOM session...");
        final JISession destructSession = this.session;
        final Thread destructor = new Thread(new Runnable() {

            public void run() {
                final long ts = System.currentTimeMillis();
                try {
                    log.debug("Starting destruction of DCOM session");
                    JISession.destroySession(destructSession);
                    log.debug("Destructed DCOM session");
                } catch (final Throwable e) {
                    log.error("Failed to destruct DCOM session", e);
                }
            }
        }, "UtgardSessionDestructor");
        destructor.setName("OPCSessionDestructor");
        destructor.setDaemon(true);
        destructor.start();
        log.debug("Destroying DCOM session... forked");

        this.errorMessageResolver = null;
        this.session = null;
        this.comServer = null;
        this.server = null;

        this.groups.clear();
    }

    /**
     * Disconnect the connection if it is connected
     */
    public synchronized void disconnect() {
        if (!isConnected()) {
            return;
        }

        try {
            notifyConnectionStateChange(false);
        } catch (final Throwable t) {
        }

        cleanup();
    }

    /**
     * Dispose the connection in the case of an error
     */
    public void dispose() {
        disconnect();
    }

    protected synchronized Group getGroup(final OPCGroupStateMgt groupMgt) throws JIException, IllegalArgumentException, UnknownHostException {
        final Integer serverHandle = groupMgt.getState().getServerHandle();
        if (this.groups.containsKey(serverHandle)) {
            return this.groups.get(serverHandle);
        } else {
            final Group group = new Group(this, serverHandle, groupMgt);
            this.groups.put(serverHandle, group);
            return group;
        }
    }

    /**
     * Add a new named group to the server
     *
     * @param name The name of the group to use. Must be unique or
     *             <code>null</code> so that the server creates a unique name.
     * @return The new group
     * @throws NotConnectedException    If the server is not connected using {@link Server#connect()}
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     * @throws DuplicateGroupException  If a group with this name already exists
     */
    public synchronized Group addGroup(final String name) throws NotConnectedException, IllegalArgumentException, UnknownHostException, JIException, DuplicateGroupException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }

        try {
            final OPCGroupStateMgt groupMgt = this.server.addGroup(name,
                    this.defaultActive, this.defaultUpdateRate, 0,
                    this.defaultTimeBias, this.defaultPercentDeadband,
                    this.defaultLocaleID);
            return getGroup(groupMgt);
        } catch (final JIException e) {
            if (e.getErrorCode() == 0xC004000C) {
                throw new DuplicateGroupException();
            }
            throw e;
        }
    }

    /**
     * Add a new group and let the server generate a group name
     * <p>
     * Actually this method only calls {@link Server#addGroup(String)} with
     * <code>null</code> as parameter.
     *
     * @return the new group
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws NotConnectedException
     * @throws JIException
     * @throws DuplicateGroupException
     */
    public Group addGroup() throws IllegalArgumentException,
            UnknownHostException, NotConnectedException, JIException,
            DuplicateGroupException {
        return addGroup(null);
    }

    /**
     * Find a group by its name
     *
     * @param name The name to look for
     * @return The group
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     * @throws UnknownGroupException    If the group was not found
     * @throws NotConnectedException    If the server is not connected
     */
    public Group findGroup(final String name) throws IllegalArgumentException, UnknownHostException, JIException, UnknownGroupException, NotConnectedException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }

        try {
            final OPCGroupStateMgt groupMgt = this.server.getGroupByName(name);
            return getGroup(groupMgt);
        } catch (final JIException e) {
            switch (e.getErrorCode()) {
                case 0x80070057:
                    throw new UnknownGroupException(name);
                default:
                    throw e;
            }
        }
    }

    public int getDefaultLocaleID() {
        return this.defaultLocaleID;
    }

    public void setDefaultLocaleID(final int defaultLocaleID) {
        this.defaultLocaleID = defaultLocaleID;
    }

    public Float getDefaultPercentDeadband() {
        return this.defaultPercentDeadband;
    }

    public void setDefaultPercentDeadband(final Float defaultPercentDeadband) {
        this.defaultPercentDeadband = defaultPercentDeadband;
    }

    public Integer getDefaultTimeBias() {
        return this.defaultTimeBias;
    }

    public void setDefaultTimeBias(final Integer defaultTimeBias) {
        this.defaultTimeBias = defaultTimeBias;
    }

    public int getDefaultUpdateRate() {
        return this.defaultUpdateRate;
    }

    public void setDefaultUpdateRate(final int defaultUpdateRate) {
        this.defaultUpdateRate = defaultUpdateRate;
    }

    public boolean isDefaultActive() {
        return this.defaultActive;
    }

    public void setDefaultActive(final boolean defaultActive) {
        this.defaultActive = defaultActive;
    }

    /**
     * Get the flat browser
     *
     * @return The flat browser or <code>null</code> if the functionality is not
     * supported
     */
    public FlatBrowser getFlatBrowser() {
        final OPCBrowseServerAddressSpace browser = this.server.getBrowser();
        if (browser == null) {
            return null;
        }

        return new FlatBrowser(browser);
    }

    /**
     * Get the tree browser
     *
     * @return The tree browser or <code>null</code> if the functionality is not
     * supported
     * @throws JIException
     */
    public TreeBrowser getTreeBrowser() throws JIException {
        final OPCBrowseServerAddressSpace browser = this.server.getBrowser();
        if (browser == null) {
            return null;
        }

        if (browser.queryOrganization() != OPCNAMESPACETYPE.OPC_NS_HIERARCHIAL) {
            return null;
        }

        return new TreeBrowser(browser);
    }

    public synchronized String getErrorMessage(final int errorCode) {
        if (this.errorMessageResolver == null) {
            return String.format("Unknown error (%08X)", errorCode);
        }

        // resolve message
        final String message = this.errorMessageResolver.getMessage(errorCode);

        // and return if successfull
        if (message != null) {
            return message;
        }

        // return default message
        return String.format("Unknown error (%08X)", errorCode);
    }

    public synchronized void addStateListener(
            final ServerConnectionStateListener listener) {
        this.stateListeners.add(listener);
        listener.connectionStateChanged(isConnected());
    }

    public synchronized void removeStateListener(
            final ServerConnectionStateListener listener) {
        this.stateListeners.remove(listener);
    }

    protected void notifyConnectionStateChange(final boolean connected) {
        final List<ServerConnectionStateListener> list = new ArrayList<ServerConnectionStateListener>(
                this.stateListeners);
        for (final ServerConnectionStateListener listener : list) {
            listener.connectionStateChanged(connected);
        }
    }

    public OPCSERVERSTATUS getServerState(final int timeout) throws Throwable {
        return new ServerStateOperation(this.server).getServerState(timeout);
    }

    public OPCSERVERSTATUS getServerState() {
        try {
            return getServerState(2500);
        } catch (final Throwable e) {
            log.error("Server connection failed", e);
            dispose();
            return null;
        }
    }

    public void removeGroup(final Group group, final boolean force)
            throws JIException {
        if (this.groups.containsKey(group.getServerHandle())) {
            this.server.removeGroup(group.getServerHandle(), force);
            this.groups.remove(group.getServerHandle());
        }
    }
}
