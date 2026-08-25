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
