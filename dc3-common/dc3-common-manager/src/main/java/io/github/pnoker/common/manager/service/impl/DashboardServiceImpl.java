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

package io.github.pnoker.common.manager.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.constant.TopologyLimits;
import io.github.pnoker.common.manager.entity.bo.dashboard.BucketRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.DailyGrowthRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.PointVolumeRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.ProfileBindingRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDeviceRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyDriverRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyPointRow;
import io.github.pnoker.common.manager.entity.bo.dashboard.TopologyProfileRow;
import io.github.pnoker.common.manager.entity.vo.dashboard.BucketVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DeviceStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DriverStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.GrowthVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyHiddenChildVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyLinkVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyNodeVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.TopologyVO;
import io.github.pnoker.common.manager.mapper.DashboardMapper;
import io.github.pnoker.common.manager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Dashboard service implementation providing home-page aggregates and topology.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    // Top-N caps, mode / range literals and cache sizing all live in
    // {@link TopologyLimits} — tuning them is a single-line change there.

    private final Cache<String, TopologyVO> topologyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(TopologyLimits.CACHE_TTL_SECONDS))
            .maximumSize(TopologyLimits.CACHE_MAX_SIZE)
            .build();

    private final DashboardMapper dashboardMapper;

    private static List<BucketVO> buckets(List<BucketRow> rows, KeyFormatter fmt) {
        List<BucketVO> out = new ArrayList<>(rows.size());
        for (BucketRow row : rows) {
            BucketVO vo = new BucketVO();
            vo.setKey(fmt.format(row.getKey()));
            vo.setCount(row.getCount());
            out.add(vo);
        }
        return out;
    }

    private static String enableKey(Object raw) {
        if (Objects.isNull(raw))
            return TopologyLimits.UNKNOWN_BUCKET;
        Byte b = raw instanceof Number n ? n.byteValue() : Byte.parseByte(raw.toString());
        EnableFlagEnum e = EnableFlagEnum.ofIndex(b);
        return Objects.isNull(e) ? TopologyLimits.UNKNOWN_BUCKET : e.name();
    }

    // ================================================================
    // Topology (GET /dashboard/topology)
    // ================================================================

    private static String driverTypeKey(Object raw) {
        if (Objects.isNull(raw))
            return TopologyLimits.UNKNOWN_BUCKET;
        Byte b = raw instanceof Number n ? n.byteValue() : Byte.parseByte(raw.toString());
        DriverTypeEnum e = DriverTypeEnum.ofIndex(b);
        return Objects.isNull(e) ? TopologyLimits.UNKNOWN_BUCKET : e.name();
    }

    /**
     * Pads a sparse (day, count) row set into a fixed-length series ending on
     * {@code today}.
     */
    private static List<Long> fillSeries(List<DailyGrowthRow> rows, LocalDate today, int length) {
        long[] series = new long[length];
        LocalDate anchor = today.minusDays(length - 1L);
        for (DailyGrowthRow row : rows) {
            LocalDate day = row.getDay();
            if (Objects.isNull(day))
                continue;
            int idx = (int) (day.toEpochDay() - anchor.toEpochDay());
            if (idx >= 0 && idx < length) {
                series[idx] = row.getCount();
            }
        }
        List<Long> out = new ArrayList<>(length);
        for (long v : series)
            out.add(v);
        return out;
    }

    private static TopologyStatsVO emptyStats(String rangeKey, boolean volumeMode) {
        TopologyStatsVO s = new TopologyStatsVO();
        s.setRangeLabel(volumeMode ? rangeLabel(rangeKey) : null);
        return s;
    }

    /**
     * Primary sort = weight desc; tie-break = id asc (stable layout).
     */
    private static <T> Comparator<T> cmpByMap(Map<Long, Long> weights, java.util.function.ToLongFunction<T> idOf) {
        return (a, b) -> {
            long wa = weights.getOrDefault(idOf.applyAsLong(a), 0L);
            long wb = weights.getOrDefault(idOf.applyAsLong(b), 0L);
            int cmp = Long.compare(wb, wa);
            if (cmp != 0)
                return cmp;
            return Long.compare(idOf.applyAsLong(a), idOf.applyAsLong(b));
        };
    }

    private static String normaliseRange(String rangeKey) {
        if (Objects.isNull(rangeKey) || rangeKey.isBlank())
            return TopologyLimits.RANGE_DEFAULT;
        return switch (rangeKey) {
            case TopologyLimits.RANGE_TODAY, TopologyLimits.RANGE_24H, TopologyLimits.RANGE_7D,
                 TopologyLimits.RANGE_30D -> rangeKey;
            default -> TopologyLimits.RANGE_DEFAULT;
        };
    }

    private static LocalDateTime fromOfRange(String rangeKey) {
        LocalDateTime now = LocalDateTime.now();
        return switch (rangeKey) {
            case TopologyLimits.RANGE_TODAY -> now.toLocalDate().atStartOfDay();
            case TopologyLimits.RANGE_24H -> now.minusHours(24);
            case TopologyLimits.RANGE_30D -> now.minusDays(30);
            default -> now.minusDays(7);
        };
    }

    private static String rangeLabel(String rangeKey) {
        return switch (rangeKey) {
            case TopologyLimits.RANGE_TODAY -> "Today";
            case TopologyLimits.RANGE_24H -> TopologyLimits.RANGE_24H;
            case TopologyLimits.RANGE_30D -> TopologyLimits.RANGE_30D;
            default -> TopologyLimits.RANGE_DEFAULT;
        };
    }

    private static long nullZero(Long v) {
        return Objects.isNull(v) ? 0L : v;
    }

    private static TopologyNodeVO node(String id, String name, int layer, String type,
                                       List<TopologyHiddenChildVO> hiddenChildren) {
        TopologyNodeVO n = new TopologyNodeVO();
        n.setId(id);
        n.setName(name);
        n.setLayer(layer);
        n.setType(type);
        n.setHiddenChildren(hiddenChildren);
        return n;
    }

    private static TopologyLinkVO link(String source, String target, long value) {
        TopologyLinkVO l = new TopologyLinkVO();
        l.setSource(source);
        l.setTarget(target);
        l.setValue(value);
        return l;
    }

    @Override
    public DriverStatsVO driverStats(Long tenantId) {
        DriverStatsVO out = new DriverStatsVO();

        List<BucketVO> byEnable = buckets(dashboardMapper.countDriverByEnable(tenantId),
                DashboardServiceImpl::enableKey);
        List<BucketVO> byType = buckets(dashboardMapper.countDriverByType(tenantId),
                DashboardServiceImpl::driverTypeKey);
        List<BucketVO> byService = buckets(dashboardMapper.countDriverByService(tenantId),
                v -> Objects.isNull(v) ? SymbolConstant.HYPHEN : v.toString());

        out.setByEnable(byEnable);
        out.setByType(byType);
        out.setByService(byService);
        out.setTotal(byEnable.stream().mapToLong(BucketVO::getCount).sum());
        return out;
    }

    @Override
    public DeviceStatsVO deviceStats(Long tenantId, int topN) {
        int clampedTopN = Math.clamp(topN, 1, 50);
        DeviceStatsVO out = new DeviceStatsVO();

        List<BucketVO> byEnable = buckets(dashboardMapper.countDeviceByEnable(tenantId),
                DashboardServiceImpl::enableKey);
        List<BucketVO> byDriver = buckets(dashboardMapper.countDeviceByDriver(tenantId, clampedTopN),
                v -> Objects.isNull(v) ? SymbolConstant.HYPHEN : v.toString());
        List<BucketVO> byProfile = buckets(dashboardMapper.countDeviceByProfile(tenantId, clampedTopN),
                v -> Objects.isNull(v) ? SymbolConstant.HYPHEN : v.toString());

        out.setByEnable(byEnable);
        out.setByDriver(byDriver);
        out.setByProfile(byProfile);
        out.setTotal(byEnable.stream().mapToLong(BucketVO::getCount).sum());
        return out;
    }

    @Override
    public GrowthVO dailyGrowth(Long tenantId, int days) {
        int clamped = Math.clamp(days, 1, 90);
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(clamped - 1L).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        GrowthVO out = new GrowthVO();
        out.setDriverDailyCounts(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_driver", from, to), today, clamped));
        out.setDeviceDailyCounts(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_device", from, to), today, clamped));
        out.setPointDailyCounts(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_point", from, to), today, clamped));
        out.setProfileDailyCounts(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_profile", from, to), today, clamped));
        return out;
    }

    @Override
    public TopologyVO topology(Long tenantId, String mode, String rangeKey) {
        String normMode = TopologyLimits.MODE_VOLUME.equalsIgnoreCase(mode) ? TopologyLimits.MODE_VOLUME
                : TopologyLimits.MODE_CARDINALITY;
        String normRange = normaliseRange(rangeKey);
        String cacheKey = tenantId + SymbolConstant.COLON + normMode + SymbolConstant.COLON + normRange;
        TopologyVO hit = topologyCache.getIfPresent(cacheKey);
        if (Objects.nonNull(hit))
            return hit;

        TopologyVO out = computeTopology(tenantId, normMode, normRange);
        topologyCache.put(cacheKey, out);
        return out;
    }

    private TopologyVO computeTopology(Long tenantId, String mode, String rangeKey) {
        TopologyVO out = new TopologyVO();
        boolean volumeMode = TopologyLimits.MODE_VOLUME.equals(mode);

        // ---- Fetch metadata (tenant-wide) --------------------------------
        // Pull everything up-front — cardinality over metadata is small, and
        // volume mode's Top-N sort needs per-entity weights anyway so a
        // single-pass rollup is simpler than iterated filtered queries.

        List<TopologyDriverRow> driverRows = dashboardMapper.topologyDrivers(tenantId);
        long driverTotal = driverRows.size();
        if (driverRows.isEmpty()) {
            out.setStats(emptyStats(rangeKey, volumeMode));
            return out;
        }

        Map<Long, String> driverNameById = new LinkedHashMap<>();
        for (TopologyDriverRow r : driverRows) {
            driverNameById.put(r.getId(), r.getDriverName());
        }
        // All driver ids are needed to fetch their devices; we crop AFTER
        // computing volumes so the top-N drivers reflect real activity, not
        // just the ones with the most devices listed.
        List<Long> allDriverIds = new ArrayList<>(driverNameById.keySet());

        List<TopologyDeviceRow> deviceRows = dashboardMapper.topologyDevicesByDrivers(tenantId, allDriverIds);
        long deviceTotal = deviceRows.size();
        Map<Long, Long> deviceToDriver = new HashMap<>(deviceRows.size() * 2);
        Map<Long, String> deviceNameById = new LinkedHashMap<>();
        for (TopologyDeviceRow r : deviceRows) {
            Long did = r.getId();
            deviceToDriver.put(did, r.getDriverId());
            deviceNameById.put(did, r.getDeviceName());
        }
        List<Long> allDeviceIds = new ArrayList<>(deviceNameById.keySet());

        List<ProfileBindingRow> bindingRows = allDeviceIds.isEmpty() ? Collections.emptyList()
                : dashboardMapper.topologyProfileBindings(tenantId, allDeviceIds);
        // device → set<profile> and profile → set<device>
        Map<Long, Set<Long>> profilesByDevice = new HashMap<>();
        Map<Long, Set<Long>> devicesByProfile = new LinkedHashMap<>();
        for (ProfileBindingRow r : bindingRows) {
            Long did = r.getDeviceId();
            Long pid = r.getProfileId();
            profilesByDevice.computeIfAbsent(did, k -> new HashSet<>()).add(pid);
            devicesByProfile.computeIfAbsent(pid, k -> new LinkedHashSet<>()).add(did);
        }

        Set<Long> allProfileIdSet = devicesByProfile.keySet();
        List<TopologyProfileRow> profileRows = allProfileIdSet.isEmpty() ? Collections.emptyList()
                : dashboardMapper.topologyProfilesByIds(tenantId, allProfileIdSet);
        Map<Long, String> profileNameById = new LinkedHashMap<>();
        for (TopologyProfileRow r : profileRows) {
            profileNameById.put(r.getId(), r.getProfileName());
        }

        List<TopologyPointRow> pointRows = allProfileIdSet.isEmpty() ? Collections.emptyList()
                : dashboardMapper.topologyPointsByProfiles(tenantId, allProfileIdSet);
        long pointTotal = pointRows.size();
        // profile → ordered list of points
        Map<Long, List<TopologyPointRow>> pointsByProfile = new LinkedHashMap<>();
        for (TopologyPointRow r : pointRows) {
            pointsByProfile.computeIfAbsent(r.getProfileId(), k -> new ArrayList<>()).add(r);
        }

        // ---- Fetch volumes if needed -------------------------------------
        // Map<(deviceId, pointId), count>. Empty in cardinality mode.
        Map<Long, Map<Long, Long>> volumeByDevicePoint = new HashMap<>();
        if (volumeMode) {
            LocalDateTime from = fromOfRange(rangeKey);
            List<PointVolumeRow> volumeRows = dashboardMapper.topologyPointVolumes(tenantId, from);
            for (PointVolumeRow r : volumeRows) {
                Long did = r.getDeviceId();
                Long pid = r.getPointId();
                long cnt = r.getCnt();
                volumeByDevicePoint.computeIfAbsent(did, k -> new HashMap<>()).put(pid, cnt);
            }
        }

        // ---- Compute per-entity weights ---------------------------------
        // In volume mode weights are pv counts; in cardinality mode they
        // degenerate to the structural counts the earlier implementation
        // used (devices per driver, profiles per device, 1 per point).

        // point weight = Σ volumes[(d, p)] over all d bound to p's profile
        Map<Long, Long> pointWeight = new HashMap<>();
        // device weight = Σ volumes[(d, p)] over all p in any profile bound
        // to d (cardinality fallback = count of profile bindings)
        Map<Long, Long> deviceWeight = new HashMap<>();
        // driver weight = Σ device weights for d.driver_id = r (cardinality
        // fallback = count of devices)
        Map<Long, Long> driverWeight = new HashMap<>();

        if (volumeMode) {
            for (Map.Entry<Long, List<TopologyPointRow>> e : pointsByProfile.entrySet()) {
                Long profileId = e.getKey();
                Set<Long> devs = devicesByProfile.getOrDefault(profileId, Collections.emptySet());
                for (TopologyPointRow r : e.getValue()) {
                    Long pid = r.getId();
                    long sum = 0;
                    for (Long did : devs) {
                        Map<Long, Long> vs = volumeByDevicePoint.get(did);
                        if (Objects.nonNull(vs))
                            sum += vs.getOrDefault(pid, 0L);
                    }
                    pointWeight.put(pid, sum);
                }
            }
            for (Map.Entry<Long, Map<Long, Long>> e : volumeByDevicePoint.entrySet()) {
                Long did = e.getKey();
                long sum = 0;
                for (long v : e.getValue().values())
                    sum += v;
                deviceWeight.put(did, sum);
                Long drvId = deviceToDriver.get(did);
                if (Objects.nonNull(drvId))
                    driverWeight.merge(drvId, sum, Long::sum);
            }
        } else {
            // Cardinality fallbacks mirror the original Top-N behaviour:
            // driver weight = count of devices
            // device weight = count of profile bindings
            // point weight = 1 (stable tie-break on id desc later)
            for (TopologyDriverRow r : driverRows) {
                driverWeight.put(r.getId(), r.getDeviceCount());
            }
            for (TopologyDeviceRow r : deviceRows) {
                deviceWeight.put(r.getId(), r.getProfileCount());
            }
            for (TopologyPointRow r : pointRows) {
                pointWeight.put(r.getId(), 1L);
            }
        }

        // ---- Volume-mode zero-weight filter -----------------------------
        // In volume mode the whole point of the chart is "where is data
        // actually flowing". Entities with zero flow — brand-new drivers
        // not yet registered, devices without a profile binding, points
        // whose sensor hasn't reported in the selected window — just add
        // visual noise (half-rendered columns with no bands). Drop them
        // here BEFORE Top-N sort so the crop reflects real activity.
        //
        // Cardinality mode keeps everything so operators can still see
        // "unconfigured" entities on the structural view.
        List<TopologyDriverRow> activeDrivers = volumeMode ? driverRows.stream()
                .filter(r -> nullZero(driverWeight.get(r.getId())) > 0)
                .collect(java.util.stream.Collectors.toList()) : new ArrayList<>(driverRows);

        // ---- Crop Top-N drivers ----------------------------------------
        activeDrivers.sort(cmpByMap(driverWeight, v -> v.getId()));
        List<TopologyDriverRow> topDrivers = activeDrivers.size() > TopologyLimits.TOP_DRIVERS
                ? activeDrivers.subList(0, TopologyLimits.TOP_DRIVERS) : activeDrivers;
        Set<Long> topDriverIds = new LinkedHashSet<>();
        for (TopologyDriverRow r : topDrivers)
            topDriverIds.add(r.getId());

        // ---- Crop Top-N devices (must belong to a kept driver) ----------
        List<TopologyDeviceRow> filteredDevices = new ArrayList<>();
        for (TopologyDeviceRow r : deviceRows) {
            if (!topDriverIds.contains(r.getDriverId()))
                continue;
            if (volumeMode && nullZero(deviceWeight.get(r.getId())) == 0)
                continue;
            filteredDevices.add(r);
        }
        filteredDevices.sort(cmpByMap(deviceWeight, v -> v.getId()));
        List<TopologyDeviceRow> topDevices = filteredDevices.size() > TopologyLimits.TOP_DEVICES
                ? filteredDevices.subList(0, TopologyLimits.TOP_DEVICES) : filteredDevices;
        Set<Long> topDeviceIdSet = new LinkedHashSet<>();
        for (TopologyDeviceRow r : topDevices)
            topDeviceIdSet.add(r.getId());

        // Per-driver "others:device:{driverId}" buckets for cropped
        // devices. In volume mode only devices that had activity but got
        // cropped land here — zero-flow devices were filtered above, so
        // Others never represents "nothing is happening".
        Map<Long, List<TopologyHiddenChildVO>> otherDevicesByDriver = new LinkedHashMap<>();
        for (TopologyDeviceRow r : filteredDevices) {
            Long deviceId = r.getId();
            if (topDeviceIdSet.contains(deviceId))
                continue;
            Long drvId = r.getDriverId();
            TopologyHiddenChildVO hidden = new TopologyHiddenChildVO();
            hidden.setId("device:" + deviceId);
            hidden.setName(r.getDeviceName());
            hidden.setType("device");
            otherDevicesByDriver.computeIfAbsent(drvId, k -> new ArrayList<>()).add(hidden);
        }

        // ---- Profile set (bound to kept devices) -----------------------
        // In volume mode drop profiles whose points collectively got no
        // samples — otherwise the profile column paints a tile with no
        // outgoing band, exactly the "empty profile" case the operator flagged.
        Set<Long> keptProfileIds = new LinkedHashSet<>();
        for (Long did : topDeviceIdSet) {
            Set<Long> pids = profilesByDevice.get(did);
            if (Objects.nonNull(pids))
                keptProfileIds.addAll(pids);
        }
        if (volumeMode) {
            Set<Long> active = new LinkedHashSet<>();
            for (Long pid : keptProfileIds) {
                long sum = 0;
                for (TopologyPointRow p : pointsByProfile.getOrDefault(pid, Collections.emptyList())) {
                    sum += nullZero(pointWeight.get(p.getId()));
                    if (sum > 0)
                        break;
                }
                if (sum > 0)
                    active.add(pid);
            }
            keptProfileIds = active;
        }

        // ---- Compose nodes + links -------------------------------------
        List<TopologyNodeVO> nodes = new ArrayList<>();
        List<TopologyLinkVO> links = new ArrayList<>();

        for (TopologyDriverRow r : topDrivers) {
            Long id = r.getId();
            nodes.add(node("driver:" + id, driverNameById.get(id), 1, "driver", null));
        }

        // Driver → Device links. Edge weight in cardinality mode = 1 (one
        // relationship); in volume mode = that device's total pv count.
        for (TopologyDeviceRow r : topDevices) {
            Long id = r.getId();
            Long driverId = r.getDriverId();
            String name = r.getDeviceName();
            nodes.add(node("device:" + id, name, 2, "device", null));
            long w = volumeMode ? nullZero(deviceWeight.get(id)) : 1L;
            links.add(link("driver:" + driverId, "device:" + id, Math.max(1L, w)));
        }

        for (Map.Entry<Long, List<TopologyHiddenChildVO>> e : otherDevicesByDriver.entrySet()) {
            Long driverId = e.getKey();
            List<TopologyHiddenChildVO> children = e.getValue();
            long w;
            if (volumeMode) {
                long sum = 0;
                for (TopologyHiddenChildVO c : children) {
                    long did = Long.parseLong(c.getId().substring("device:".length()));
                    sum += nullZero(deviceWeight.get(did));
                }
                w = Math.max(1L, sum);
            } else {
                w = children.size();
            }
            String otherId = "others:device:" + driverId;
            nodes.add(node(otherId, "Others (" + children.size() + ")", 2, "others", children));
            links.add(link("driver:" + driverId, otherId, w));
        }

        // Profile nodes
        for (Long profileId : keptProfileIds) {
            String name = profileNameById.getOrDefault(profileId, String.valueOf(profileId));
            nodes.add(node("profile:" + profileId, name, 3, "profile", null));
        }

        // Device → Profile links — one per (device, profile) binding where
        // the device was kept. Volume-mode weight = Σ pv over the points
        // on that profile for that device. Skip the edge entirely when
        // that sum is 0 so we don't draw a dangling minimum-width band
        // for a device that never sent data through this profile — even
        // if other devices on the same profile did.
        for (ProfileBindingRow r : bindingRows) {
            Long deviceId = r.getDeviceId();
            Long profileId = r.getProfileId();
            if (!topDeviceIdSet.contains(deviceId))
                continue;
            if (!keptProfileIds.contains(profileId))
                continue;
            long w = 1L;
            if (volumeMode) {
                long sum = 0;
                Map<Long, Long> devVols = volumeByDevicePoint.get(deviceId);
                List<TopologyPointRow> pts = pointsByProfile.getOrDefault(profileId, Collections.emptyList());
                if (Objects.nonNull(devVols)) {
                    for (TopologyPointRow p : pts) {
                        sum += devVols.getOrDefault(p.getId(), 0L);
                    }
                }
                if (sum == 0)
                    continue;
                w = sum;
            }
            links.add(link("device:" + deviceId, "profile:" + profileId, w));
        }

        // Profile → Point layer with per-profile Top-N and Others. In
        // volume mode we drop points whose weight is 0 up front so the
        // profile never renders a slim tile for "this point is declared
        // but silent"; cardinality keeps everything for the structural
        // view.
        for (Long profileId : keptProfileIds) {
            List<TopologyPointRow> allPoints = new ArrayList<>(
                    pointsByProfile.getOrDefault(profileId, Collections.emptyList()));
            if (volumeMode) {
                allPoints.removeIf(p -> nullZero(pointWeight.get(p.getId())) == 0);
                allPoints.sort(cmpByMap(pointWeight, v -> v.getId()));
            } else {
                allPoints.sort(Comparator.comparingLong((TopologyPointRow r) -> r.getId()).reversed());
            }

            int keep = Math.min(TopologyLimits.TOP_POINTS_PER_PROFILE, allPoints.size());
            for (int i = 0; i < keep; i++) {
                TopologyPointRow r = allPoints.get(i);
                Long id = r.getId();
                String name = r.getPointName();
                nodes.add(node("point:" + id, name, 4, "point", null));
                long w = volumeMode ? nullZero(pointWeight.get(id)) : 1L;
                links.add(link("profile:" + profileId, "point:" + id, Math.max(1L, w)));
            }

            if (allPoints.size() > keep) {
                List<TopologyHiddenChildVO> children = new ArrayList<>(allPoints.size() - keep);
                long sumW = 0;
                for (int i = keep; i < allPoints.size(); i++) {
                    TopologyPointRow r = allPoints.get(i);
                    Long id = r.getId();
                    TopologyHiddenChildVO hidden = new TopologyHiddenChildVO();
                    hidden.setId("point:" + id);
                    hidden.setName(r.getPointName());
                    hidden.setType("point");
                    children.add(hidden);
                    if (volumeMode)
                        sumW += nullZero(pointWeight.get(id));
                }
                long w = volumeMode ? Math.max(1L, sumW) : children.size();
                String otherId = "others:point:" + profileId;
                nodes.add(node(otherId, "Others (" + children.size() + ")", 4, "others", children));
                links.add(link("profile:" + profileId, otherId, w));
            }
        }

        // ---- stats ----
        TopologyStatsVO stats = new TopologyStatsVO();
        stats.setDriverCount(driverTotal);
        stats.setDeviceCount(deviceTotal);
        stats.setProfileCount(allProfileIdSet.size());
        stats.setPointCount(pointTotal);
        stats.setRangeLabel(volumeMode ? rangeLabel(rangeKey) : null);

        out.setNodes(nodes);
        out.setLinks(links);
        out.setStats(stats);
        return out;
    }

    @FunctionalInterface
    private interface KeyFormatter {

        String format(Object raw);

    }

}
