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

-- History hypertables are treated as append-only data here.
-- Keep operate_time for compatibility, but do not maintain UPDATE triggers.
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
    device_id BIGINT DEFAULT 0 NOT NULL,                        -- Device ID
    point_id  BIGINT DEFAULT 0 NOT NULL,                        -- Point ID
    raw_value TEXT   DEFAULT ''::TEXT          NOT NULL,        -- Raw value as captured from the device
    cal_value TEXT   DEFAULT ''::TEXT          NOT NULL,        -- Calculated/transformed value
    num_value DOUBLE PRECISION,                                 -- Best-effort numeric projection of cal_value (NULL for non-numeric payloads)
    driver_id BIGINT DEFAULT 0 NOT NULL,                        -- Driver ID
    tenant_id BIGINT DEFAULT 0 NOT NULL,                        -- Tenant ID
    create_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL, -- Creation time
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

COMMENT
ON TABLE dc3_point_value IS 'Device point history hypertable; single source of truth across all point types';
COMMENT
ON COLUMN dc3_point_value.device_id IS 'Device ID';
COMMENT
ON COLUMN dc3_point_value.point_id IS 'Point ID';
COMMENT
ON COLUMN dc3_point_value.raw_value IS 'Raw value as captured from the device';
COMMENT
ON COLUMN dc3_point_value.cal_value IS 'Calculated/transformed value';
COMMENT
ON COLUMN dc3_point_value.num_value IS 'Best-effort numeric projection of cal_value (NULL for non-numeric payloads)';
COMMENT
ON COLUMN dc3_point_value.driver_id IS 'Driver ID';
COMMENT
ON COLUMN dc3_point_value.tenant_id IS 'Tenant ID';
COMMENT
ON COLUMN dc3_point_value.create_time IS 'Creation time';
COMMENT
ON COLUMN dc3_point_value.operate_time IS 'Operation time';

SELECT *
FROM public.create_hypertable('dc3_point_value', public.by_range('create_time', INTERVAL '1 day'));
SELECT *
FROM public.add_dimension('dc3_point_value', public.by_hash('device_id', 16));

ALTER TABLE dc3_point_value
SET(
        timescaledb.compress,
        timescaledb.compress_segmentby = 'tenant_id,device_id,point_id',
        timescaledb.compress_orderby = 'create_time DESC'
);
SELECT public.add_compression_policy('dc3_point_value', INTERVAL '7 days');
SELECT public.add_retention_policy('dc3_point_value', INTERVAL '180 days');
