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
package io.github.pnoker.db.r2dbc.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuration properties bound to the runtime prefix. */
@ConfigurationProperties(prefix = "dc3.r2dbc")
public class R2dbcRuntimeProperties {

    private String schemaFingerprint;
    private String schemaContract = "r2dbc-flag-day-v1";
    private String idFormat = "uuidv7-bigint";
    private String timeFormat = "utc-micros";
    private String jsonFormat = "canonical-v1";
    private java.time.Duration startupTimeout = java.time.Duration.ofSeconds(10);

    /** Return the schema fingerprint the startup gate requires. */
    public String getSchemaFingerprint() {
        return schemaFingerprint;
    }

    /** Set the schema fingerprint the startup gate requires. */
    public void setSchemaFingerprint(String schemaFingerprint) {
        this.schemaFingerprint = schemaFingerprint;
    }

    /** Return the schema contract identifier. */
    public String getSchemaContract() {
        return schemaContract;
    }

    /** Set the schema contract identifier. */
    public void setSchemaContract(String schemaContract) {
        this.schemaContract = schemaContract;
    }

    /** Return the id format identifier. */
    public String getIdFormat() {
        return idFormat;
    }

    /** Set the id format identifier. */
    public void setIdFormat(String idFormat) {
        this.idFormat = idFormat;
    }

    /** Return the time format identifier. */
    public String getTimeFormat() {
        return timeFormat;
    }

    /** Set the time format identifier. */
    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    /** Return the JSON format identifier. */
    public String getJsonFormat() {
        return jsonFormat;
    }

    /** Set the JSON format identifier. */
    public void setJsonFormat(String jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    /** Return the startup gate timeout. */
    public java.time.Duration getStartupTimeout() {
        return startupTimeout;
    }

    /** Set the startup gate timeout. */
    public void setStartupTimeout(java.time.Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
