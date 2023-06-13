/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.serotonin.modbus4j.ip;

import com.serotonin.modbus4j.base.ModbusUtils;

/**
 * <p>IpParameters class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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
