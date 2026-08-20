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

CREATE SCHEMA IF NOT EXISTS dc3_history;
SET search_path TO dc3_history, public;

-- History hypertables are append-only. create_time is the device acquisition
-- timestamp; operate_time is the platform persistence timestamp. Writers set
-- both explicitly, so history tables do not need UPDATE triggers.
--
-- Storage model: a single dc3_point_value hypertable holds every sample
-- regardless of declared point type. The textual raw_value / cal_value
-- columns preserve the original payload, while num_value carries the
-- best-effort parsed double for aggregation. Application writers populate
-- num_value when cal_value parses cleanly as a double; non-numeric and
-- JSON payloads leave it NULL. Aggregate queries (AVG/MIN/MAX/SUM/timeseries)
-- filter on num_value IS NOT NULL to skip text payloads cheaply.

-- ----------------------------
-- Table structure for dc3_point_value
-- ----------------------------
CREATE TABLE dc3_point_value
(
    message_id     TEXT        NOT NULL,                         -- Immutable event identity
    schema_version INTEGER     NOT NULL,                         -- Point-value wire schema version
    driver_node    TEXT        NOT NULL,                         -- Producing driver runtime node
    sequence       BIGINT      NOT NULL,                         -- Monotonic sequence within driver_node
    fencing_token  BIGINT      NOT NULL,                         -- Manager-issued ownership fence
    device_id      BIGINT      DEFAULT 0 NOT NULL,               -- Device ID
    point_id       BIGINT      DEFAULT 0 NOT NULL,               -- Point ID
    raw_value      TEXT        DEFAULT ''::TEXT NOT NULL,        -- Raw value as captured from the device
    cal_value      TEXT        DEFAULT ''::TEXT NOT NULL,        -- Calculated/transformed value
    num_value      DOUBLE PRECISION,                             -- Best-effort numeric projection of cal_value (NULL for non-numeric payloads)
    quality        INTEGER     DEFAULT 0 NOT NULL,               -- OPC-UA style quality code, 0 = GOOD (S17)
    driver_id      BIGINT      DEFAULT 0 NOT NULL,               -- Driver ID
    tenant_id      BIGINT      DEFAULT 0 NOT NULL,               -- Tenant ID
    create_time    TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Creation time
    operate_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL -- Operation time
);

CREATE INDEX idx_point_value_device_point_time ON dc3_point_value (device_id, point_id, create_time DESC);
-- Primary tenant-scoped time-series index. Supports aggregateInWindow, samplesInWindow, and latest-value lookups.
CREATE INDEX idx_point_value_ts_lookup ON dc3_point_value (tenant_id, device_id, point_id, create_time DESC);
CREATE INDEX idx_point_value_num_time ON dc3_point_value (device_id, point_id, create_time DESC) WHERE num_value IS NOT NULL;
-- Dashboard and history pages scan tenant-scoped time windows and latest streams.
CREATE INDEX idx_point_value_tenant_time ON dc3_point_value (tenant_id, create_time DESC);
-- Coverage-gap checks probe existence by tenant and point without a time bound.
CREATE INDEX idx_point_value_tenant_point ON dc3_point_value (tenant_id, point_id);

COMMENT ON TABLE dc3_point_value IS 'Device point history hypertable; single source of truth across all point types';
COMMENT ON COLUMN dc3_point_value.message_id IS 'Immutable event identity';
COMMENT ON COLUMN dc3_point_value.schema_version IS 'Point-value wire schema version';
COMMENT ON COLUMN dc3_point_value.driver_node IS 'Producing driver runtime node';
COMMENT ON COLUMN dc3_point_value.sequence IS 'Monotonic sequence within the producing driver node';
COMMENT ON COLUMN dc3_point_value.fencing_token IS 'Manager-issued ownership fencing token';
COMMENT ON COLUMN dc3_point_value.device_id IS 'Device ID';
COMMENT ON COLUMN dc3_point_value.point_id IS 'Point ID';
COMMENT ON COLUMN dc3_point_value.raw_value IS 'Raw value as captured from the device';
COMMENT ON COLUMN dc3_point_value.cal_value IS 'Calculated/transformed value';
COMMENT ON COLUMN dc3_point_value.num_value IS 'Best-effort numeric projection of cal_value (NULL for non-numeric payloads)';
COMMENT ON COLUMN dc3_point_value.quality IS 'OPC-UA style quality code, 0 = GOOD';
COMMENT ON COLUMN dc3_point_value.driver_id IS 'Driver ID';
COMMENT ON COLUMN dc3_point_value.tenant_id IS 'Tenant ID';
COMMENT ON COLUMN dc3_point_value.create_time IS 'Creation time';
COMMENT ON COLUMN dc3_point_value.operate_time IS 'Operation time';

SELECT *
FROM public.create_hypertable('dc3_point_value', public.by_range('create_time', INTERVAL '1 day'));
SELECT *
FROM public.add_dimension('dc3_point_value', public.by_hash('device_id', 16));

-- TimescaleDB requires every partitioning column in a unique index. The
-- (series, device-time) key backs the TSDB port's natural upsert — duplicate
-- samples on the same (tenant, device, point, create_time) update in place,
-- last write wins. The pre-port message_id event index was retired with the
-- repository layer: replay dedup now lives in the ingest idempotency window
-- and message_id remains a plain traceability column.
CREATE UNIQUE INDEX uk_point_value_series_time
    ON dc3_point_value (tenant_id, device_id, point_id, create_time);

ALTER TABLE dc3_point_value
    SET (
        timescaledb.compress,
        timescaledb.compress_segmentby = 'tenant_id,device_id,point_id',
        timescaledb.compress_orderby = 'create_time DESC'
    );
SELECT public.add_compression_policy('dc3_point_value', INTERVAL '7 days');
SELECT public.add_retention_policy('dc3_point_value', INTERVAL '180 days');

-- ----------------------------
-- Transactional latest-value projection
-- ----------------------------
-- This is a normal PostgreSQL table, not an in-process cache. Every Data Center
-- replica reads and writes the same projection. History and latest are updated in
-- one transaction, and older/out-of-order readings cannot overwrite newer values.
CREATE TABLE dc3_point_latest
(
    tenant_id      BIGINT      NOT NULL,                  -- Tenant ID
    device_id      BIGINT      NOT NULL,                  -- Device ID
    point_id       BIGINT      NOT NULL,                  -- Point ID
    message_id     TEXT        NOT NULL,                  -- Immutable event identity
    schema_version INTEGER     NOT NULL,                  -- Point-value wire schema version
    driver_node    TEXT        NOT NULL,                  -- Producing driver runtime node
    sequence       BIGINT      NOT NULL,                  -- Monotonic sequence within driver_node
    fencing_token  BIGINT      NOT NULL,                  -- Manager-issued ownership fence
    raw_value      TEXT        DEFAULT ''::TEXT NOT NULL, -- Raw value as captured from the device
    cal_value      TEXT        DEFAULT ''::TEXT NOT NULL, -- Calculated/transformed value
    num_value      DOUBLE PRECISION,                      -- Best-effort numeric projection of cal_value
    driver_id      BIGINT      DEFAULT 0 NOT NULL,        -- Driver ID
    create_time    TIMESTAMPTZ NOT NULL,                  -- Device acquisition time
    operate_time   TIMESTAMPTZ NOT NULL,                  -- Platform persistence time
    PRIMARY KEY (tenant_id, device_id, point_id)
);

CREATE INDEX idx_point_latest_driver ON dc3_point_latest (tenant_id, driver_id);

COMMENT ON TABLE dc3_point_latest IS 'Transactional latest point value projection shared by all Data Center replicas';
COMMENT ON COLUMN dc3_point_latest.tenant_id IS 'Tenant ID';
COMMENT ON COLUMN dc3_point_latest.device_id IS 'Device ID';
COMMENT ON COLUMN dc3_point_latest.point_id IS 'Point ID';
COMMENT ON COLUMN dc3_point_latest.message_id IS 'Immutable event identity';
COMMENT ON COLUMN dc3_point_latest.schema_version IS 'Point-value wire schema version';
COMMENT ON COLUMN dc3_point_latest.driver_node IS 'Producing driver runtime node';
COMMENT ON COLUMN dc3_point_latest.sequence IS 'Monotonic sequence within the producing driver node';
COMMENT ON COLUMN dc3_point_latest.fencing_token IS 'Manager-issued ownership fencing token';
COMMENT ON COLUMN dc3_point_latest.raw_value IS 'Raw value as captured from the device';
COMMENT ON COLUMN dc3_point_latest.cal_value IS 'Calculated/transformed value';
COMMENT ON COLUMN dc3_point_latest.num_value IS 'Best-effort numeric projection of cal_value (NULL for non-numeric payloads)';
COMMENT ON COLUMN dc3_point_latest.driver_id IS 'Driver ID';
COMMENT ON COLUMN dc3_point_latest.create_time IS 'Device acquisition time';
COMMENT ON COLUMN dc3_point_latest.operate_time IS 'Platform persistence time';
