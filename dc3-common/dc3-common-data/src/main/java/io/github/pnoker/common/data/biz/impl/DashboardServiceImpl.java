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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.biz.DashboardService;
import io.github.pnoker.common.data.entity.vo.dashboard.*;
import io.github.pnoker.common.data.mapper.AlertMapper;
import io.github.pnoker.common.data.mapper.DashboardMapper;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.github.pnoker.common.data.constant.DashboardLimits.*;

/**
 * Business service implementation for dashboard aggregation operations.
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    /**
     * Dimensions whose column name we interpolate directly into the GROUP BY. Only these
     * are accepted — never pass user input through unchecked.
     */
    private static final Map<String, String> DIMENSION_COLUMN = Map.of("device", "device_id", "point", "point_id",
            "driver", "driver_id");

    private static final Set<String> GRANULARITY = Set.of("hour", "day");

    /**
     * Whitelist for the alert source parameter.
     */
    private static final Set<String> ALERT_SOURCES = Set.of(SOURCE_DEVICE, SOURCE_DRIVER, SOURCE_POINT);

    private final DashboardMapper dashboardMapper;

    private final AlertMapper alertMapper;

    private final DeviceFacade deviceFacade;

    private final PointFacade pointFacade;

    private final DriverFacade driverFacade;

    /**
     * BucketRow.key is Object (shared across SMALLINT / VARCHAR / BIGINT group columns);
     * stringify for the VO.
     */
    private static String asString(Object v) {
        return Objects.isNull(v) ? null : v.toString();
    }

    @Override
    public List<LatencyBucketVO> latencyHistogram(Long tenantId, int rangeHours) {
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        var rows = dashboardMapper.latencyHistogram(tenantId, from, to);
        // Pad missing bins with zero so the UI always gets six buckets.
        long[] counts = new long[6];
        for (var row : rows) {
            int bin = row.getBin();
            if (bin >= 0 && bin < counts.length) {
                counts[bin] = row.getCount();
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
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        var rows = dashboardMapper.hourlyActivity(tenantId, from, to);
        long[][] grid = new long[7][24];
        for (var row : rows) {
            int dow = row.getDow();
            int hour = row.getHour();
            if (dow >= 0 && dow < 7 && hour >= 0 && hour < 24) {
                grid[dow][hour] = row.getCount();
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

    @Override
    public Page<AlertItemVO> alertPage(Long tenantId, String source, Integer alarmTypeFlag, Integer confirmFlag,
                                       LocalDateTime from, long current, long size) {
        String src = Objects.isNull(source) || source.isBlank() ? null : (ALERT_SOURCES.contains(source) ? source : null);
        long clampedCurrent = Math.max(1L, current);
        long clampedSize = Math.clamp(size, 1L, MAX_PAGE_SIZE);
        long offset = (clampedCurrent - 1L) * clampedSize;

        long total = alertMapper.countFiltered(tenantId, src, alarmTypeFlag, confirmFlag, from);
        var rows = alertMapper.listPaged(tenantId, src, alarmTypeFlag, confirmFlag, from, offset, clampedSize);
        List<AlertItemVO> records = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertItemVO vo = new AlertItemVO();
            vo.setId(row.getId());
            vo.setSource(row.getSource());
            vo.setSourceId(row.getSourceId());
            vo.setPointId(row.getPointId());
            vo.setAlarmTypeFlag(row.getAlarmTypeFlag());
            vo.setConfirmFlag(row.getConfirmFlag());
            vo.setCreateTime(row.getCreateTime());
            vo.setMessage(row.getMessage());
            records.add(vo);
        }
        // Use MyBatis-Plus Page so the JSON shape matches every other list
        // endpoint in the project (current / size / total / pages / records).
        Page<AlertItemVO> page = new Page<>(clampedCurrent, clampedSize, total);
        page.setRecords(records);
        return page;
    }

    @Override
    public boolean confirmAlert(Long tenantId, String source, Long id) {
        if (Objects.isNull(source) || !ALERT_SOURCES.contains(source) || Objects.isNull(id)) {
            return false;
        }
        return alertMapper.confirmOne(tenantId, source, id) > 0;
    }

    @Override
    public boolean unconfirmAlert(Long tenantId, String source, Long id) {
        if (Objects.isNull(source) || !ALERT_SOURCES.contains(source) || Objects.isNull(id)) {
            return false;
        }
        return alertMapper.unconfirmOne(tenantId, source, id) > 0;
    }

    @Override
    public int bulkConfirmAlert(Long tenantId, List<AlertBulkConfirmRequest.Item> items, boolean confirm) {
        if (Objects.isNull(items) || items.isEmpty())
            return 0;
        int changed = 0;
        for (AlertBulkConfirmRequest.Item item : items) {
            if (Objects.isNull(item) || Objects.isNull(item.getSource()) || Objects.isNull(item.getId()))
                continue;
            String source = item.getSource();
            if (!ALERT_SOURCES.contains(source))
                continue;
            long id = item.getId();
            changed += confirm ? alertMapper.confirmOne(tenantId, source, id)
                    : alertMapper.unconfirmOne(tenantId, source, id);
        }
        return changed;
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
    public long countTotal(Long tenantId) {
        return dashboardMapper.countTotal(tenantId);
    }

    @Override
    public List<TimeseriesPointVO> timeseries(Long tenantId, String granularity, int rangeHours) {
        String g = GRANULARITY.contains(granularity) ? granularity : "hour";
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);
        String bucket = "1 " + g;

        var rows = dashboardMapper.timeseries(tenantId, from, to, bucket);
        List<TimeseriesPointVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            TimeseriesPointVO vo = new TimeseriesPointVO();
            vo.setBucket(row.getBucket());
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<TopEntityVO> top(Long tenantId, String dimension, int rangeHours, int limit) {
        String column = DIMENSION_COLUMN.get(dimension);
        if (Objects.isNull(column)) {
            throw new IllegalArgumentException("Unsupported dimension: " + dimension);
        }
        int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
        int hours = Math.clamp(rangeHours, 1, MAX_HOURS_90D);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(hours);

        var rows = dashboardMapper.top(tenantId, column, from, to, clampedLimit);
        List<TopEntityVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            TopEntityVO vo = new TopEntityVO();
            vo.setEntityId(row.getEntityId());
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<LatestPointValueVO> latestStream(Long tenantId, int size) {
        int clamped = Math.clamp(size, 1, MAX_LIVE_SIZE);
        var rows = dashboardMapper.latestStream(tenantId, clamped);
        List<LatestPointValueVO> out = new ArrayList<>(rows.size());
        Set<Long> deviceIds = new HashSet<>();
        Set<Long> pointIds = new HashSet<>();
        Set<Long> driverIds = new HashSet<>();
        for (var row : rows) {
            LatestPointValueVO vo = new LatestPointValueVO();
            vo.setDeviceId(row.getDeviceId());
            vo.setPointId(row.getPointId());
            vo.setDriverId(row.getDriverId());
            vo.setRawValue(row.getRawValue());
            vo.setCalValue(row.getCalValue());
            vo.setValueType(row.getValueType());
            vo.setCreateTime(row.getCreateTime());
            out.add(vo);
            if (Objects.nonNull(vo.getDeviceId()) && vo.getDeviceId() > 0)
                deviceIds.add(vo.getDeviceId());
            if (Objects.nonNull(vo.getPointId()) && vo.getPointId() > 0)
                pointIds.add(vo.getPointId());
            if (Objects.nonNull(vo.getDriverId()) && vo.getDriverId() > 0)
                driverIds.add(vo.getDriverId());
        }

        // Point-value tables live in the history data source; device / point /
        // driver metadata lives in the master data source (and in remote
        // Manager in distributed deployments), so we cannot JOIN them in SQL.
        // Resolve names in bulk — local facade does it in one SQL, gRPC fans
        // out concurrently — to avoid the per-id round-trip storm.
        Map<Long, String> deviceNames = deviceFacade.listByIds(tenantId, deviceIds).stream()
                .collect(java.util.stream.Collectors.toMap(FacadeDeviceBO::getId, FacadeDeviceBO::getDeviceName, (a, b) -> a));
        Map<Long, String> pointNames = pointFacade.listByIds(tenantId, pointIds).stream()
                .collect(java.util.stream.Collectors.toMap(FacadePointBO::getId, FacadePointBO::getPointName, (a, b) -> a));
        Map<Long, String> driverNames = driverFacade.listByIds(tenantId, driverIds).stream()
                .collect(java.util.stream.Collectors.toMap(FacadeDriverBO::getId, FacadeDriverBO::getDriverName, (a, b) -> a));

        for (LatestPointValueVO vo : out) {
            if (Objects.nonNull(vo.getDeviceId()))
                vo.setDeviceName(deviceNames.get(vo.getDeviceId()));
            if (Objects.nonNull(vo.getPointId()))
                vo.setPointName(pointNames.get(vo.getPointId()));
            if (Objects.nonNull(vo.getDriverId()))
                vo.setDriverName(driverNames.get(vo.getDriverId()));
        }

        return out;
    }

    @Override
    public AlertStatsVO alertStats(Long tenantId) {
        AlertStatsVO vo = new AlertStatsVO();
        var totals = alertMapper.countAll(tenantId);
        if (Objects.nonNull(totals)) {
            vo.setTotal(totals.getTotal());
            vo.setUnconfirmed(totals.getUnconfirmed());
        }
        var rows = alertMapper.countByType(tenantId);
        List<AlertStatsVO.BucketVO> buckets = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertStatsVO.BucketVO b = new AlertStatsVO.BucketVO();
            b.setKey(asString(row.getKey()));
            b.setCount(row.getCount());
            buckets.add(b);
        }
        vo.setByType(buckets);

        for (var row : alertMapper.countBySource(tenantId)) {
            String src = row.getSource();
            long srcTotal = row.getTotal();
            long srcUnconfirmed = row.getUnconfirmed();
            if ("device".equals(src)) {
                vo.setDeviceAlerts(srcTotal);
                vo.setDeviceUnconfirmed(srcUnconfirmed);
            } else if ("driver".equals(src)) {
                vo.setDriverAlerts(srcTotal);
                vo.setDriverUnconfirmed(srcUnconfirmed);
            }
        }

        // Today's ALARM counts per source
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        for (var row : alertMapper.todayBySource(tenantId, todayStart)) {
            String src = row.getSource();
            long srcTotal = row.getTotal();
            long srcUnconfirmed = row.getUnconfirmed();
            if ("device".equals(src)) {
                vo.setTodayDeviceAlarms(srcTotal);
                vo.setTodayDeviceUnconfirmed(srcUnconfirmed);
            } else if ("driver".equals(src)) {
                vo.setTodayDriverAlarms(srcTotal);
                vo.setTodayDriverUnconfirmed(srcUnconfirmed);
            }
        }

        // 24-hour hourly sparkline, anchored to top-of-hour now-23.
        LocalDateTime anchor = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0).minusHours(23);
        long[] series = new long[24];
        for (var row : alertMapper.hourlyCounts(tenantId, anchor)) {
            LocalDateTime bucket = row.getBucket();
            if (Objects.isNull(bucket))
                continue;
            long diffHours = java.time.Duration.between(anchor, bucket).toHours();
            int idx = (int) diffHours;
            if (idx >= 0 && idx < series.length) {
                series[idx] = row.getCount();
            }
        }
        List<Long> sparkline = new ArrayList<>(series.length);
        for (long v : series)
            sparkline.add(v);
        vo.setSparkline24h(sparkline);
        return vo;
    }

    @Override
    public List<AlertItemVO> alertLatest(Long tenantId, int size) {
        int clamped = Math.clamp(size, 1, MAX_LIMIT);
        var rows = alertMapper.latest(tenantId, clamped);
        List<AlertItemVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertItemVO vo = new AlertItemVO();
            vo.setId(row.getId());
            vo.setSource(row.getSource());
            vo.setSourceId(row.getSourceId());
            vo.setPointId(row.getPointId());
            vo.setAlarmTypeFlag(row.getAlarmTypeFlag());
            vo.setConfirmFlag(row.getConfirmFlag());
            vo.setCreateTime(row.getCreateTime());
            vo.setMessage(row.getMessage());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<AlertTrendVO> alertTrend(Long tenantId, int days) {
        int clamped = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(clamped).atTime(LocalTime.MIN);
        var rows = alertMapper.dailyTrend(tenantId, from);
        List<AlertTrendVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertTrendVO vo = new AlertTrendVO();
            vo.setDate(row.getDate());
            vo.setDeviceCount(row.getDeviceCount());
            vo.setDriverCount(row.getDriverCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<AlertTopSourceVO> alertTopSources(Long tenantId, int days, int limit) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDate.now().minusDays(clampedDays).atTime(LocalTime.MIN);
        var rows = alertMapper.topSources(tenantId, from, clampedLimit);
        List<AlertTopSourceVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertTopSourceVO vo = new AlertTopSourceVO();
            vo.setSource(row.getSource());
            vo.setSourceId(row.getSourceId());
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<AlertActivityCellVO> alertActivity(Long tenantId, int days) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(clampedDays).atTime(LocalTime.MIN);
        var rows = alertMapper.activityHeatmap(tenantId, from);
        long[][] grid = new long[7][24];
        for (var row : rows) {
            int dow = row.getDow();
            int hour = row.getHour();
            if (dow >= 0 && dow < 7 && hour >= 0 && hour < 24) {
                grid[dow][hour] = row.getCount();
            }
        }
        // Zero-pad every cell so the UI always receives 7 × 24 = 168 rows.
        List<AlertActivityCellVO> out = new ArrayList<>(7 * 24);
        for (int d = 0; d < 7; d++) {
            for (int h = 0; h < 24; h++) {
                AlertActivityCellVO vo = new AlertActivityCellVO();
                vo.setDow(d);
                vo.setHour(h);
                vo.setCount(grid[d][h]);
                out.add(vo);
            }
        }
        return out;
    }

    @Override
    public List<AlertTypeBucketVO> alertTypeDistribution(Long tenantId, int days) {
        int clampedDays = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(clampedDays).atTime(LocalTime.MIN);
        var rows = alertMapper.typeDistribution(tenantId, from);
        List<AlertTypeBucketVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertTypeBucketVO vo = new AlertTypeBucketVO();
            vo.setType(Objects.isNull(row.getKey()) ? null : row.getKey().toString());
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<AlertTopSourceVO> alertStormSources(Long tenantId, int hours, int minCount, int limit) {
        int clampedHours = Math.clamp(hours, 1, MAX_HOURS_30D);
        int clampedMin = Math.max(1, minCount);
        int clampedLimit = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDateTime.now().minusHours(clampedHours);
        var rows = alertMapper.stormSources(tenantId, from, clampedMin, clampedLimit);
        List<AlertTopSourceVO> out = new ArrayList<>(rows.size());
        for (var row : rows) {
            AlertTopSourceVO vo = new AlertTopSourceVO();
            vo.setSource(row.getSource());
            vo.setSourceId(row.getSourceId());
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    // ================================================================
    // Phase-2 insights
    // ================================================================

    @Override
    public List<FlappingSourceVO> alertFlapping(Long tenantId, int hours, int minCount, int limit) {
        int h = Math.clamp(hours, 1, MAX_HOURS_7D);
        int min = Math.max(MIN_FLAPPING_COUNT, minCount);
        int lim = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDateTime.now().minusHours(h);
        var rows = alertMapper.flappingSources(tenantId, from, min, lim);
        List<FlappingSourceVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            FlappingSourceVO vo = new FlappingSourceVO();
            vo.setSource(r.getSource());
            vo.setSourceId(r.getSourceId());
            vo.setAlarmTypeFlag(r.getAlarmTypeFlag());
            vo.setCount(r.getCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<CorrelationPairVO> alertCorrelation(Long tenantId, int hours, int windowSec, int limit) {
        int h = Math.clamp(hours, 1, MAX_HOURS_7D);
        int w = Math.clamp(windowSec, MIN_CORRELATION_WINDOW_SEC, MAX_CORRELATION_WINDOW_SEC);
        int lim = Math.clamp(limit, 1, MAX_CORRELATION_PAIRS);
        LocalDateTime from = LocalDateTime.now().minusHours(h);
        var rows = alertMapper.correlationPairs(tenantId, from, w, lim);
        List<CorrelationPairVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            CorrelationPairVO vo = new CorrelationPairVO();
            vo.setASource(r.getASource());
            vo.setASourceId(r.getASourceId());
            vo.setAEventType(r.getAEventType());
            vo.setBSource(r.getBSource());
            vo.setBSourceId(r.getBSourceId());
            vo.setBEventType(r.getBEventType());
            vo.setCoCount(r.getCoCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<PeerDeviationVO> alertPeerDeviation(Long tenantId, int days) {
        int d = Math.clamp(days, 1, MAX_PEER_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(d).atTime(LocalTime.MIN);
        var rows = alertMapper.peerAlarmCounts(tenantId, from);

        // Group by profile → list of (device, alarmCount); then pick median
        // and flag devices with count >= 3x median (and a floor of 5 alarms
        // so a profile with median=1 doesn't emit noise).
        Map<Long, List<long[]>> byProfile = new HashMap<>();
        for (var r : rows) {
            long prof = r.getProfileId();
            long dev = r.getDeviceId();
            long cnt = r.getAlarmCount();
            byProfile.computeIfAbsent(prof, k -> new ArrayList<>()).add(new long[]{dev, cnt});
        }
        List<PeerDeviationVO> out = new ArrayList<>();
        for (Map.Entry<Long, List<long[]>> e : byProfile.entrySet()) {
            List<long[]> devs = e.getValue();
            if (devs.size() < 3)
                continue; // need enough peers for a peer test
            long[] sorted = devs.stream().mapToLong(a -> a[1]).sorted().toArray();
            long median = sorted[sorted.length / 2];
            for (long[] a : devs) {
                if (a[1] < 5)
                    continue;
                if (median > 0 && a[1] < median * 3)
                    continue;
                if (median == 0 && a[1] < 5)
                    continue;
                PeerDeviationVO vo = new PeerDeviationVO();
                vo.setProfileId(e.getKey());
                vo.setDeviceId(a[0]);
                vo.setAlarmCount(a[1]);
                vo.setPeerMedian(median);
                vo.setRatio(median == 0 ? 0.0 : Math.round((double) a[1] / median * 100.0) / 100.0);
                out.add(vo);
            }
        }
        out.sort((a, b) -> Long.compare(b.getAlarmCount(), a.getAlarmCount()));
        // Cap to 50 to keep payload bounded
        return out.size() > 50 ? out.subList(0, 50) : out;
    }

    @Override
    public AgingBacklogVO alertAgingBacklog(Long tenantId) {
        var row = alertMapper.agingBuckets(tenantId);
        AgingBacklogVO vo = new AgingBacklogVO();
        if (Objects.nonNull(row)) {
            vo.setUnder1h(row.getUnder1h());
            vo.setH1to6(row.getH1to6());
            vo.setH6to24(row.getH6to24());
            vo.setOver24h(row.getOver24h());
            vo.setTotal(row.getTotal());
        }
        return vo;
    }

    @Override
    public List<MttaTrendVO> alertMtta(Long tenantId, int days) {
        int d = Math.clamp(days, 1, MAX_DAYS);
        LocalDateTime from = LocalDate.now().minusDays(d).atTime(LocalTime.MIN);
        var rows = alertMapper.mttaByDay(tenantId, from);
        List<MttaTrendVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            MttaTrendVO vo = new MttaTrendVO();
            vo.setDate(r.getDate());
            vo.setP50Ms(r.getP50Ms());
            vo.setP95Ms(r.getP95Ms());
            vo.setConfirmedCount(r.getConfirmedCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<ProtocolHealthVO> protocolHealth(Long tenantId) {
        var rows = alertMapper.protocolHealth(tenantId);
        List<ProtocolHealthVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            ProtocolHealthVO vo = new ProtocolHealthVO();
            vo.setServiceName(r.getServiceName());
            vo.setDriverCount(r.getDriverCount());
            vo.setEnabledCount(r.getEnabledCount());
            vo.setDeviceCount(r.getDeviceCount());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<ChangeImpactVO> changeImpact(Long tenantId, int days, int limit) {
        int d = Math.clamp(days, 1, MAX_DAYS);
        int lim = Math.clamp(limit, 1, MAX_LIMIT);
        LocalDateTime from = LocalDate.now().minusDays(d).atTime(LocalTime.MIN);
        var rows = alertMapper.recentChanges(tenantId, from, lim);
        List<ChangeImpactVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            ChangeImpactVO vo = new ChangeImpactVO();
            vo.setKind(r.getKind());
            vo.setEntityId(r.getEntityId());
            vo.setOperateTime(r.getOperateTime());
            out.add(vo);
        }
        return out;
    }

    @Override
    public List<SilentSourceVO> silentSources(Long tenantId, int baselineDays, int silentMinutes, int limit) {
        int baseline = Math.clamp(baselineDays, 1, MAX_BASELINE_DAYS);
        int silent = Math.clamp(silentMinutes, 5, 60 * 24);
        int lim = Math.clamp(limit, 1, MAX_COVERAGE_GAP_LIMIT);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusDays(baseline);
        LocalDateTime silentThreshold = now.minusMinutes(silent);

        var rows = dashboardMapper.silentSources(tenantId, from, silentThreshold, lim);
        List<SilentSourceVO> out = new ArrayList<>(rows.size());
        for (var r : rows) {
            SilentSourceVO vo = new SilentSourceVO();
            vo.setDeviceId(r.getDeviceId());
            vo.setPointId(r.getPointId());
            LocalDateTime last = r.getLastSeen();
            vo.setLastSeen(last);
            if (Objects.nonNull(last)) {
                vo.setSilentSeconds(java.time.Duration.between(last, now).getSeconds());
            }
            out.add(vo);
        }
        return out;
    }

    @Override
    public CoverageGapVO coverageGap(Long tenantId, int limit) {
        int lim = Math.clamp(limit, 1, MAX_COVERAGE_GAP_LIMIT);
        CoverageGapVO vo = new CoverageGapVO();
        vo.setTotalPoints(dashboardMapper.countPointsInTenant(tenantId));
        var rows = dashboardMapper.coverageGapItems(tenantId, lim);
        for (var r : rows) {
            CoverageGapVO.Item it = new CoverageGapVO.Item();
            it.setPointId(r.getPointId());
            it.setProfileId(r.getProfileId());
            vo.getItems().add(it);
        }
        // missingPoints = actual count; items may be capped. Use a second
        // query only if we hit the cap — otherwise items.size() is authoritative.
        vo.setMissingPoints(vo.getItems().size());
        return vo;
    }

}
