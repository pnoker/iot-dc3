package io.github.pnoker.db.r2dbc.runtime.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dc3.r2dbc")
public class R2dbcRuntimeProperties {

    private String schemaFingerprint;
    private String schemaContract = "r2dbc-flag-day-v1";
    private String idFormat = "uuidv7";
    private String timeFormat = "utc-micros";
    private String jsonFormat = "canonical-v1";
    private java.time.Duration startupTimeout = java.time.Duration.ofSeconds(10);

    public String getSchemaFingerprint() {
        return schemaFingerprint;
    }

    public void setSchemaFingerprint(String schemaFingerprint) {
        this.schemaFingerprint = schemaFingerprint;
    }

    public String getSchemaContract() {
        return schemaContract;
    }

    public void setSchemaContract(String schemaContract) {
        this.schemaContract = schemaContract;
    }

    public String getIdFormat() {
        return idFormat;
    }

    public void setIdFormat(String idFormat) {
        this.idFormat = idFormat;
    }

    public String getTimeFormat() {
        return timeFormat;
    }

    public void setTimeFormat(String timeFormat) {
        this.timeFormat = timeFormat;
    }

    public String getJsonFormat() {
        return jsonFormat;
    }

    public void setJsonFormat(String jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    public java.time.Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(java.time.Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }
}
