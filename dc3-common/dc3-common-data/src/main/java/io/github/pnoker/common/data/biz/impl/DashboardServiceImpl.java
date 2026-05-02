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

package io.github.pnoker.common.data.biz.impl;

import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.entity.vo.dashboard.*;
import io.github.pnoker.common.data.mapper.AlertMapper;
import io.github.pnoker.common.data.mapper.DashboardMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    /**
     * Dimensions whose column name we interpolate directly into the GROUP BY.
     * Only these are accepted — never pass user input through unchecked.
     */
    private static final Map<String, String> DIMENSION_COLUMN = Map.of(
            "device", "device_id",
            "point", "point_id",
            "driver", "driver_id"
    );

    private static final Set<String> GRANULARITY = Set.of("hour", "day");

    @Resource
    private DashboardMapper dashboardMapper;

    @Resource
    private AlertMapper alertMapper;

    @Override
    public List<LatencyBucketVO> latencyHistogram(Long tenantId, int rangeHours) {
        int hours = Math.max(1, Math.min(rangeHours, 24 * 90));
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        List<Map<String, Object>> rows = dashboardMapper.latencyHistogram(tenantId, from, to);
        // Pad missing bins with zero so the UI always gets six buckets.
        long[] counts = new long[6];
        for (Map<String, Object> row : rows) {
            int bin = toInt(row.get("bin"));
            if (bin >= 0 && bin < counts.length) {
                counts[bin] = toLong(row.get("count"));
            }
        }
        List<LatencyBucketVO> out = new ArrayList<>(counts.length);
        for (int i = 0; i < counts.length; i++) {
            LatencyBucketVO vo = new LatencyBucketVO();
            vo.setBin(i);
            vo.setCount(counts[i]);
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<ActivityCellVO> hourlyActivity(Long tenantId, int rangeHours) {
        int hours = Math.max(1, Math.min(rangeHours, 24 * 90));
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        List<Map<String, Object>> rows = dashboardMapper.hourlyActivity(tenantId, from, to);
        long[][] grid = new long[7][24];
        for (Map<String, Object> row : rows) {
            int dow = toInt(row.get("dow"));
            int hour = toInt(row.get("hour"));
            if (dow >= 0 && dow < 7 && hour >= 0 && hour < 24) {
                grid[dow][hour] = toLong(row.get("count"));
            }
        }
        List<ActivityCellVO> out = new ArrayList<>(7 * 24);
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                ActivityCellVO vo = new ActivityCellVO();
                vo.setDow(d);
                vo.setHour(h);
                vo.setCount(grid[d][h]);
                out.add(vo);
            }
        }
        return out;
    }

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
    }

    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime ldt) return ldt;
        if (v instanceof Timestamp ts) return ts.toLocalDateTime();
        // Fallback for JDBC drivers that return java.time.OffsetDateTime etc.
        if (v instanceof java.time.OffsetDateTime odt) return odt.toLocalDateTime();
        return LocalDateTime.parse(v.toString());
    }

    // Kept for reference — zero-arg unused reference suppressing unused-import warning.
    @SuppressWarnings("unused")
    private static LocalDateTime startOfDay() {
        return LocalDate.now().atTime(LocalTime.MIN);
    }

    @Override
    public long countToday(Long tenantId) {
        LocalDateTime from = LocalDate.now().atStartOfDay();
        LocalDateTime to = LocalDateTime.now();
        return dashboardMapper.countInRange(tenantId, from, to);
    }

    @Override
    public long countYesterday(Long tenantId) {
        LocalDateTime from = LocalDate.now().minusDays(1).atStartOfDay();
        LocalDateTime to = LocalDate.now().atStartOfDay();
        return dashboardMapper.countInRange(tenantId, from, to);
    }

    @Override
    public List<TimeseriesPointVO> timeseries(Long tenantId, String granularity, int rangeHours) {
        String g = GRANULARITY.contains(granularity) ? granularity : "hour";
        int hours = Math.max(1, Math.min(rangeHours, 24 * 90));
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        String bucket = "1 " + g;

        List<Map<String, Object>> rows = dashboardMapper.timeseries(tenantId, from, to, bucket);
        List<TimeseriesPointVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            TimeseriesPointVO vo = new TimeseriesPointVO();
            vo.setBucket(toLocalDateTime(row.get("bucket")));
            vo.setCount(toLong(row.get("count")));
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<TopEntityVO> top(Long tenantId, String dimension, int rangeHours, int limit) {
        String column = DIMENSION_COLUMN.get(dimension);
        if (column == null) {
            throw new IllegalArgumentException("Unsupported dimension: " + dimension);
        }
        int clampedLimit = Math.max(1, Math.min(limit, 50));
        int hours = Math.max(1, Math.min(rangeHours, 24 * 90));
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);

        List<Map<String, Object>> rows = dashboardMapper.top(tenantId, column, from, to, clampedLimit);
        List<TopEntityVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            TopEntityVO vo = new TopEntityVO();
            vo.setEntityId(toLong(row.get("entity_id")));
            vo.setCount(toLong(row.get("count")));
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<LatestPointValueVO> latestStream(Long tenantId, int size) {
        int clamped = Math.max(1, Math.min(size, 100));
        List<Map<String, Object>> rows = dashboardMapper.latestStream(tenantId, clamped);
        List<LatestPointValueVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            LatestPointValueVO vo = new LatestPointValueVO();
            vo.setDeviceId(toLong(row.get("device_id")));
            vo.setPointId(toLong(row.get("point_id")));
            vo.setDriverId(toLong(row.get("driver_id")));
            vo.setRawValue(asString(row.get("raw_value")));
            vo.setCalValue(asString(row.get("cal_value")));
            vo.setValueType(asString(row.get("value_type")));
            vo.setCreateTime(toLocalDateTime(row.get("create_time")));
            out.add(vo);
        }
        return out;
    }

    @Override
    public AlertStatsVO alertStats(Long tenantId) {
        AlertStatsVO vo = new AlertStatsVO();
        Map<String, Object> totals = alertMapper.countAll(tenantId);
        if (totals != null) {
            vo.setTotal(toLong(totals.get("total")));
            vo.setUnconfirmed(toLong(totals.get("unconfirmed")));
        }
        List<Map<String, Object>> rows = alertMapper.countByType(tenantId);
        List<AlertStatsVO.BucketVO> buckets = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            AlertStatsVO.BucketVO b = new AlertStatsVO.BucketVO();
            b.setKey(asString(row.get("key")));
            b.setCount(toLong(row.get("count")));
            buckets.add(b);
        }
        vo.setByType(buckets);

        // 24-hour hourly sparkline, anchored to top-of-hour now-23.
        LocalDateTime anchor = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(23);
        long[] series = new long[24];
        for (Map<String, Object> row : alertMapper.hourlyCounts(tenantId, anchor)) {
            LocalDateTime bucket = toLocalDateTime(row.get("bucket"));
            if (bucket == null) continue;
            long diffHours = java.time.Duration.between(anchor, bucket).toHours();
            int idx = (int) diffHours;
            if (idx >= 0 && idx < series.length) {
                series[idx] = toLong(row.get("count"));
            }
        }
        List<Long> sparkline = new ArrayList<>(series.length);
        for (long v : series) sparkline.add(v);
        vo.setSparkline24h(sparkline);
        return vo;
    }

    @Override
    public List<AlertItemVO> alertLatest(Long tenantId, int size) {
        int clamped = Math.max(1, Math.min(size, 50));
        List<Map<String, Object>> rows = alertMapper.latest(tenantId, clamped);
        List<AlertItemVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            AlertItemVO vo = new AlertItemVO();
            vo.setId(toLong(row.get("id")));
            vo.setSource(asString(row.get("source")));
            vo.setSourceId(toLong(row.get("source_id")));
            vo.setPointId(toLong(row.get("point_id")));
            vo.setEventTypeFlag(toInt(row.get("event_type_flag")));
            vo.setConfirmFlag(toInt(row.get("confirm_flag")));
            vo.setCreateTime(toLocalDateTime(row.get("create_time")));
            out.add(vo);
        }
        return out;
    }
}
