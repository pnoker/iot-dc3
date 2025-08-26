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
package com.serotonin.modbus4j.ip;

import com.serotonin.modbus4j.base.ModbusUtils;

/**
 * <p>IpParameters class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class IpParameters {
    private String host;
    private int port = ModbusUtils.TCP_PORT;
    private boolean encapsulated;
    private Integer lingerTime = -1;


    /**
     * <p>Getter for the field <code>host</code>.</p>
     *
     * @return a {@link String} object.
     */
    public String getHost() {
        return host;
    }

    /**
     * <p>Setter for the field <code>host</code>.</p>
     *
     * @param host a {@link String} object.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * <p>Getter for the field <code>port</code>.</p>
     *
     * @return a int.
     */
    public int getPort() {
        return port;
    }

    /**
     * <p>Setter for the field <code>port</code>.</p>
     *
     * @param port a int.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * <p>isEncapsulated.</p>
     *
     * @return a boolean.
     */
    public boolean isEncapsulated() {
        return encapsulated;
    }

    /**
     * <p>Setter for the field <code>encapsulated</code>.</p>
     *
     * @param encapsulated a boolean.
     */
    public void setEncapsulated(boolean encapsulated) {
        this.encapsulated = encapsulated;
    }

    /**
     * <p>Getter for the field <code>linger</code>.</p>
     *
     * @return a int.
     */
    public Integer getLingerTime() {
        return lingerTime;
    }

    /**
     * <p>Setter for the field <code>linger</code>.</p>
     *
     * @param lingerTime a int.
     */
    public void setLingerTime(Integer lingerTime) {
        this.lingerTime = lingerTime;
    }


}
