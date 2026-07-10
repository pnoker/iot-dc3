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

-- Point-value observability layer: continuous aggregates over the
-- dc3_point_value hypertable, plus a device-metadata view for Grafana.
--
-- Design notes:
--   * Two-level hierarchical cagg: 1-minute raw rollup -> 1-hour rollup.
--     The 1h cagg is built ON TOP of the 1m cagg so long-range dashboard
--     queries (7d / 30d) re-aggregate already-summarized buckets instead
--     of scanning the full hypertable.
--   * Both caggs are realtime aggregates (materialized_only=false): recent
--     samples that fall in the not-yet-materialized tail are merged live,
--     so dashboards see fresh data without waiting for the refresh policy.
--   * Aggregation slices by (tenant_id, driver_id, device_id, point_id),
--     matching the hypertable's compress_segmentby. This is the natural
--     drill-down axis for "per driver / per device / per point" observation.
--   * num_value carries the best-effort numeric projection of cal_value;
--     AVG/MIN/MAX/SUM only touch rows where it is NOT NULL (mirrors the
--     raw-table partial index idx_point_value_num_time). first()/last()
--     retain cal_value for string-typed points so they remain observable.
--   * point and device relate via profile_id only (a profile groups many
--     devices AND many points); point_value rows already carry both ids,
--     so dashboards JOIN metadata by those ids rather than a static map.

SET search_path TO dc3_history, public;

-- ----------------------------
-- 1-minute continuous aggregate
-- ----------------------------
-- One row per (tenant, driver, device, point, minute). Powers the
-- per-point trend panel and the global ingest-rate panel.
CREATE
MATERIALIZED VIEW cagg_point_value_1m
    WITH (timescaledb.continuous = true) AS
SELECT time_bucket(INTERVAL '1 minute', create_time) AS bucket,
       tenant_id,
       driver_id,
       device_id,
       point_id,
       count(*)                                      AS sample_count,
       avg(num_value)                                AS num_avg,
       min(num_value)                                AS num_min,
       max(num_value)                                AS num_max,
       sum(num_value)                                AS num_sum,
       first(cal_value, create_time)                 AS cal_first,
       last(cal_value, create_time)                  AS cal_last
FROM dc3_point_value
GROUP BY bucket, tenant_id, driver_id, device_id, point_id
WITH NO DATA;

-- Drill-down index mirrors the dashboard filter shape.
CREATE INDEX idx_cagg_pv_1m_lookup
    ON cagg_point_value_1m (tenant_id, device_id, point_id, bucket DESC);

-- Realtime aggregate: also read the raw tail not yet materialized.
ALTER
MATERIALIZED VIEW cagg_point_value_1m
SET(timescaledb.materialized_only = false);

-- ----------------------------
-- 1-hour continuous aggregate (hierarchical)
-- ----------------------------
-- Built on the 1m cagg, so a 30-day range scans ~720 hourly rows per point
-- instead of ~43k minute rows. One row per (tenant, driver, device, point, hour).
CREATE
MATERIALIZED VIEW cagg_point_value_1h
    WITH (timescaledb.continuous = true) AS
SELECT time_bucket(INTERVAL '1 hour', bucket) AS bucket,
       tenant_id,
       driver_id,
       device_id,
       point_id,
       sum(sample_count)                      AS sample_count,
       avg(num_avg)                           AS num_avg, -- mean of minute means
       min(num_min)                           AS num_min,
       max(num_max)                           AS num_max,
       sum(num_sum)                           AS num_sum,
       first(cal_first, bucket)               AS cal_first,
       last(cal_last, bucket)                 AS cal_last
FROM cagg_point_value_1m
GROUP BY bucket, tenant_id, driver_id, device_id, point_id
WITH NO DATA;

CREATE INDEX idx_cagg_pv_1h_lookup
    ON cagg_point_value_1h (tenant_id, device_id, point_id, bucket DESC);

ALTER
MATERIALIZED VIEW cagg_point_value_1h
SET(timescaledb.materialized_only = false);

-- ----------------------------
-- Refresh policies
-- ----------------------------
-- Background refresh of the materialized window. Realtime mode still
-- answers queries from the live tail; the policy only backfills durable
-- buckets. start_offset must stay within the hypertable retention window
-- (180 days, see 05-iot-dc3-history.sql).
SELECT add_continuous_aggregate_policy('cagg_point_value_1m',
                                       start_offset = > INTERVAL '7 days',
                                       end_offset = > INTERVAL '1 minute',
                                       schedule_interval = > INTERVAL '1 minute');

SELECT add_continuous_aggregate_policy('cagg_point_value_1h',
                                       start_offset = > INTERVAL '180 days',
                                       end_offset = > INTERVAL '5 minutes',
                                       schedule_interval = > INTERVAL '5 minutes');

-- ----------------------------
-- Device metadata view for Grafana
-- ----------------------------
-- Deterministic join (device.driver_id is a single value): attaches the
-- driver name to each device so dashboard device pickers show the owning
-- driver without an extra join. Point/profile names are joined ad-hoc in
-- dashboard queries via the point_id/profile_id on each value row.
-- enable_flag is inverted in this schema: 0 = enabled, 1 = disabled.
CREATE OR REPLACE VIEW v_device_metadata AS
SELECT d.id             AS device_id,
       d.tenant_id,
       d.device_name,
       d.device_code,
       d.driver_id,
       d.profile_id,
       d.enable_flag,
       drv.driver_name,
       drv.driver_code,
       drv.service_name AS driver_service
FROM dc3_manager.dc3_device d
         LEFT JOIN dc3_manager.dc3_driver drv
                   ON drv.id = d.driver_id
                       AND drv.tenant_id = d.tenant_id
                       AND drv.deleted = 0
WHERE d.deleted = 0;
