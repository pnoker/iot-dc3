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

package org.openscada.opc.lib.list;

import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIClsid;
import org.jinterop.dcom.core.JIComServer;
import org.jinterop.dcom.core.JISession;
import org.openscada.opc.dcom.list.ClassDetails;
import org.openscada.opc.dcom.list.Constants;
import org.openscada.opc.dcom.list.impl.OPCServerList;
import rpc.core.UUID;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A wrapper around the {@link OPCServerList} class which makes the handling somewhat easier.
 *
 * @author Jens Reimann &lt;jens.reimann@th4-systems.com&gt;
 * @since 0.1.8
 */
public class ServerList {
    private final JISession _session;

    private final OPCServerList _serverList;

    /**
     * Create a new instance with an already existing session
     *
     * @param session the DCOM session
     * @param host    the host to query
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     */
    public ServerList(final JISession session, final String host) throws IllegalArgumentException, UnknownHostException, JIException {
        this._session = session;
        JIComServer comServer = new JIComServer(JIClsid.valueOf(Constants.OPCServerList_CLSID), host, this._session);
        this._serverList = new OPCServerList(comServer.createInstance());
    }

    /**
     * Create a new instance and a new DCOM session
     *
     * @param host     the host to contact
     * @param user     the user to use for authentication
     * @param password the password to use for authentication
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     */
    public ServerList(final String host, final String user, final String password) throws IllegalArgumentException, UnknownHostException, JIException {
        this(host, user, password, null);
    }

    /**
     * Create a new instance and a new DCOM session
     *
     * @param host     the host to contact
     * @param user     the user to use for authentication
     * @param password the password to use for authentication
     * @param domain   The domain to use for authentication
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     */
    public ServerList(final String host, final String user, final String password, final String domain) throws IllegalArgumentException, UnknownHostException, JIException {
        this(JISession.createSession(domain, user, password), host);
    }

    /**
     * Get the details of a opc class
     *
     * @param clsId the class to request details for
     * @return The class details
     * @throws JIException
     */
    public ClassDetails getDetails(final String clsId) throws JIException {
        return this._serverList.getClassDetails(JIClsid.valueOf(clsId));
    }

    /**
     * Fetch the class id of a prog id
     *
     * @param progId The prog id to look up
     * @return the class id or <code>null</code> if none could be found.
     * @throws JIException
     */
    public String getClsIdFromProgId(final String progId) throws JIException {
        JIClsid cls = this._serverList.getCLSIDFromProgID(progId);
        if (cls == null) {
            return null;
        }
        return cls.getCLSID();
    }

    /**
     * List all servers that match the requirements
     *
     * @param implemented All implemented categories
     * @param required    All required categories
     * @return A collection of <q>class ids</q>
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     */
    public Collection<String> listServers(final Category[] implemented, final Category[] required) throws IllegalArgumentException, UnknownHostException, JIException {
        // convert the type safe categories to plain UUIDs
        UUID[] u1 = new UUID[implemented.length];
        UUID[] u2 = new UUID[required.length];

        for (int i = 0; i < implemented.length; i++) {
            u1[i] = new UUID(implemented[i].toString());
        }

        for (int i = 0; i < required.length; i++) {
            u2[i] = new UUID(required[i].toString());
        }

        // get them as UUIDs
        Collection<UUID> resultU = this._serverList.enumClassesOfCategories(u1, u2).asCollection();

        // and convert to easier usable strings
        Collection<String> result = new ArrayList<String>(resultU.size());
        for (UUID uuid : resultU) {
            result.add(uuid.toString());
        }
        return result;
    }

    /**
     * List all servers that match the requirements and return the class details
     *
     * @param implemented All implemented categories
     * @param required    All required categories
     * @return a collection of matching server and their class information
     * @throws IllegalArgumentException
     * @throws UnknownHostException
     * @throws JIException
     */
    public Collection<ClassDetails> listServersWithDetails(final Category[] implemented, final Category[] required) throws IllegalArgumentException, UnknownHostException, JIException {
        Collection<String> resultString = listServers(implemented, required);

        List<ClassDetails> result = new ArrayList<ClassDetails>(resultString.size());

        for (String clsId : resultString) {
            result.add(getDetails(clsId));
        }

        return result;
    }
}
