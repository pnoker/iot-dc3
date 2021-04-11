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

package org.openscada.opc.lib.common;

/**
 * Holds the connection information
 *
 * @author Jens Reimann <jens.reimann@th4-systems.com>
 * <p>
 * If both <code>clsId</code> and <code>progId</code> are set then <code>clsId</code>
 * has priority!
 */
public class ConnectionInformation {
    private String _host = "localhost";

    private String _domain = "localhost";

    private String _user = "";

    private String _password = "";

    private String _clsid = null;

    private String _progId = null;

    public ConnectionInformation() {
        super();
    }

    public ConnectionInformation(String host, String clsid, String user, String password) {
        this._host = host;
        this._clsid = clsid;
        this._user = user;
        this._password = password;
    }

    public ConnectionInformation(final String user, final String password) {
        super();
        this._user = user;
        this._password = password;
    }

    public ConnectionInformation(final ConnectionInformation arg0) {
        super();
        this._user = arg0._user;
        this._password = arg0._password;
        this._domain = arg0._domain;
        this._host = arg0._host;
        this._progId = arg0._progId;
        this._clsid = arg0._clsid;
    }

    public String getDomain() {
        return this._domain;
    }

    /**
     * Set the domain of the user used for logging on
     *
     * @param domain
     */
    public void setDomain(final String domain) {
        this._domain = domain;
    }

    public String getHost() {
        return this._host;
    }

    /**
     * Set the host on which the server is located
     *
     * @param host The host to use, either an IP address oder hostname
     */
    public void setHost(final String host) {
        this._host = host;
    }

    public String getPassword() {
        return this._password;
    }

    public void setPassword(final String password) {
        this._password = password;
    }

    public String getUser() {
        return this._user;
    }

    public void setUser(final String user) {
        this._user = user;
    }

    public String getClsid() {
        return this._clsid;
    }

    public void setClsid(final String clsid) {
        this._clsid = clsid;
    }

    public String getProgId() {
        return this._progId;
    }

    public void setProgId(final String progId) {
        this._progId = progId;
    }

    public String getClsOrProgId() {
        if (this._clsid != null) {
            return this._clsid;
        } else if (this._progId != null) {
            return this._progId;
        } else {
            return null;
        }
    }
}
