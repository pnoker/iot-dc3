--
-- MySQL 8 seed for IoT DC3 — dc3_history database.
--
-- With a MySQL relational core the time-series store MUST be external
-- (TDengine / InfluxDB / IoTDB — docs/tsdb-stores.md): TimescaleDB only exists
-- inside PostgreSQL, so this database carries nothing but the transactional
-- dc3_point_latest projection, which stays relational on every deployment
-- (docs/design/tsdb-abstraction.md §9.1). The PostgreSQL seed's hypertable,
-- compression, retention and continuous-aggregate DDL have no counterpart
-- here by design.
--

CREATE DATABASE IF NOT EXISTS dc3_history
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dc3_history;

CREATE TABLE dc3_point_latest
(
    tenant_id      BIGINT              NOT NULL,
    device_id      BIGINT              NOT NULL,
    point_id       BIGINT              NOT NULL,
    message_id     TEXT                NOT NULL,
    schema_version INTEGER             NOT NULL,
    driver_node    TEXT                NOT NULL,
    sequence       BIGINT              NOT NULL,
    fencing_token  BIGINT              NOT NULL,
    raw_value      TEXT   DEFAULT ('') NOT NULL,
    cal_value      TEXT   DEFAULT ('') NOT NULL,
    num_value      DOUBLE PRECISION,
    driver_id      BIGINT DEFAULT 0    NOT NULL,
    create_time    DATETIME(6)         NOT NULL,
    operate_time   DATETIME(6)         NOT NULL,
    PRIMARY KEY (tenant_id, device_id, point_id)
);

CREATE INDEX idx_point_latest_driver ON dc3_point_latest (tenant_id, driver_id);
