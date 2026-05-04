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
import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.vo.dashboard.*;
import io.github.pnoker.common.manager.mapper.DashboardMapper;
import io.github.pnoker.common.manager.service.DashboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    /**
     * Server-side Top-N caps. Tightened here so payload + render cost stay
     * bounded on large tenants. Orders of magnitude:
     * worst-case nodes ≈ TOP_DRIVERS + TOP_DEVICES + |bound profiles| +
     * (TOP_POINTS_PER_PROFILE × |bound profiles|) + Others
     * ≈ 10 + 20 + ~30 + 15×30 + ~20 ≈ 530 (G2 sankey comfortable).
     */
    private static final int TOP_DRIVERS = 10;
    private static final int TOP_DEVICES = 20;
    private static final int TOP_POINTS_PER_PROFILE = 15;

    // ---- internal helpers -----------------------------------------------
    private static final String MODE_VOLUME = "volume";
    private static final String MODE_CARDINALITY = "cardinality";
    /**
     * 60s TTL is long enough to hide UI spam refresh bursts but short enough
     * that edits to drivers / devices / points surface on the next natural
     * poll. Cap at 200 entries so a busy multi-tenant instance doesn't bloat
     * heap — each (tenant × mode × rangeKey) triple is one entry.
     */
    private final Cache<String, TopologyVO> topologyCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(60))
            .maximumSize(200)
            .build();
    @Resource
    private DashboardMapper dashboardMapper;

    private static List<BucketVO> buckets(List<Map<String, Object>> rows, String keyCol, KeyFormatter fmt) {
        List<BucketVO> out = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            BucketVO vo = new BucketVO();
            vo.setKey(fmt.format(row.get(keyCol)));
            vo.setCount(toLong(row.get("count")));
            out.add(vo);
        }
        return out;
    }

    private static long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
    }

    private static String enableKey(Object raw) {
        if (raw == null) return "UNKNOWN";
        Byte b = raw instanceof Number n ? n.byteValue() : Byte.parseByte(raw.toString());
        EnableFlagEnum e = EnableFlagEnum.ofIndex(b);
        return e == null ? "UNKNOWN" : e.name();
    }

    // ================================================================
    // Topology (GET /dashboard/topology)
    // ================================================================

    private static String driverTypeKey(Object raw) {
        if (raw == null) return "UNKNOWN";
        Byte b = raw instanceof Number n ? n.byteValue() : Byte.parseByte(raw.toString());
        DriverTypeFlagEnum e = DriverTypeFlagEnum.ofIndex(b);
        return e == null ? "UNKNOWN" : e.name();
    }

    /**
     * Pads a sparse (day, count) row set into a fixed-length series ending on
     * {@code today}. JDBC returns DATE as java.sql.Date or LocalDate depending
     * on the driver, so handle both.
     */
    private static List<Long> fillSeries(List<Map<String, Object>> rows, LocalDate today, int length) {
        long[] series = new long[length];
        LocalDate anchor = today.minusDays(length - 1L);
        for (Map<String, Object> row : rows) {
            LocalDate day = toLocalDate(row.get("day"));
            if (day == null) continue;
            int idx = (int) (day.toEpochDay() - anchor.toEpochDay());
            if (idx >= 0 && idx < length) {
                series[idx] = toLong(row.get("count"));
            }
        }
        List<Long> out = new ArrayList<>(length);
        for (long v : series) out.add(v);
        return out;
    }

    private static LocalDate toLocalDate(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate ld) return ld;
        if (v instanceof Date d) return d.toLocalDate();
        return LocalDate.parse(v.toString());
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
            if (cmp != 0) return cmp;
            return Long.compare(idOf.applyAsLong(a), idOf.applyAsLong(b));
        };
    }

    private static String normaliseRange(String rangeKey) {
        if (rangeKey == null || rangeKey.isBlank()) return "7d";
        return switch (rangeKey) {
            case "today", "24h", "7d", "30d" -> rangeKey;
            default -> "7d";
        };
    }

    private static LocalDateTime fromOfRange(String rangeKey) {
        LocalDateTime now = LocalDateTime.now();
        return switch (rangeKey) {
            case "today" -> now.toLocalDate().atStartOfDay();
            case "24h" -> now.minusHours(24);
            case "30d" -> now.minusDays(30);
            default -> now.minusDays(7);
        };
    }

    private static String rangeLabel(String rangeKey) {
        return switch (rangeKey) {
            case "today" -> "Today";
            case "24h" -> "24h";
            case "30d" -> "30d";
            default -> "7d";
        };
    }

    private static long nullZero(Long v) {
        return v == null ? 0L : v;
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

    private static String asString(Object v) {
        return v == null ? "-" : v.toString();
    }

    @Override
    public DriverStatsVO driverStats(Long tenantId) {
        DriverStatsVO out = new DriverStatsVO();

        List<BucketVO> byEnable = buckets(
                dashboardMapper.countDriverByEnable(tenantId),
                "enable_flag",
                DashboardServiceImpl::enableKey);
        List<BucketVO> byType = buckets(
                dashboardMapper.countDriverByType(tenantId),
                "driver_type_flag",
                DashboardServiceImpl::driverTypeKey);
        List<BucketVO> byService = buckets(
                dashboardMapper.countDriverByService(tenantId),
                "service_name",
                v -> v == null ? "-" : v.toString());

        out.setByEnable(byEnable);
        out.setByType(byType);
        out.setByService(byService);
        out.setTotal(byEnable.stream().mapToLong(BucketVO::getCount).sum());
        return out;
    }

    @Override
    public DeviceStatsVO deviceStats(Long tenantId, int topN) {
        int clampedTopN = Math.max(1, Math.min(topN, 50));
        DeviceStatsVO out = new DeviceStatsVO();

        List<BucketVO> byEnable = buckets(
                dashboardMapper.countDeviceByEnable(tenantId),
                "enable_flag",
                DashboardServiceImpl::enableKey);
        List<BucketVO> byDriver = buckets(
                dashboardMapper.countDeviceByDriver(tenantId, clampedTopN),
                "driver_id",
                v -> v == null ? "-" : v.toString());
        List<BucketVO> byProfile = buckets(
                dashboardMapper.countDeviceByProfile(tenantId, clampedTopN),
                "profile_id",
                v -> v == null ? "-" : v.toString());

        out.setByEnable(byEnable);
        out.setByDriver(byDriver);
        out.setByProfile(byProfile);
        out.setTotal(byEnable.stream().mapToLong(BucketVO::getCount).sum());
        return out;
    }

    @Override
    public GrowthVO dailyGrowth(Long tenantId, int days) {
        int clamped = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now();
        LocalDateTime from = today.minusDays(clamped - 1L).atStartOfDay();
        LocalDateTime to = today.plusDays(1).atStartOfDay();

        GrowthVO out = new GrowthVO();
        out.setDriver(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_driver", from, to), today, clamped));
        out.setDevice(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_device", from, to), today, clamped));
        out.setPoint(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_point", from, to), today, clamped));
        out.setProfile(fillSeries(dashboardMapper.dailyGrowth(tenantId, "dc3_profile", from, to), today, clamped));
        return out;
    }

    @Override
    public TopologyVO topology(Long tenantId, String mode, String rangeKey) {
        String normMode = MODE_VOLUME.equalsIgnoreCase(mode) ? MODE_VOLUME : MODE_CARDINALITY;
        String normRange = normaliseRange(rangeKey);
        String cacheKey = tenantId + ":" + normMode + ":" + normRange;
        TopologyVO hit = topologyCache.getIfPresent(cacheKey);
        if (hit != null) return hit;

        TopologyVO out = computeTopology(tenantId, normMode, normRange);
        topologyCache.put(cacheKey, out);
        return out;
    }

    private TopologyVO computeTopology(Long tenantId, String mode, String rangeKey) {
        TopologyVO out = new TopologyVO();
        boolean volumeMode = MODE_VOLUME.equals(mode);

        // ---- Fetch metadata (tenant-wide) --------------------------------
        // Pull everything up-front — cardinality over metadata is small, and
        // volume mode's Top-N sort needs per-entity weights anyway so a
        // single-pass rollup is simpler than iterated filtered queries.

        List<Map<String, Object>> driverRows = dashboardMapper.topologyDrivers(tenantId);
        long driverTotal = driverRows.size();
        if (driverRows.isEmpty()) {
            out.setStats(emptyStats(rangeKey, volumeMode));
            return out;
        }

        Map<Long, String> driverNameById = new LinkedHashMap<>();
        for (Map<String, Object> r : driverRows) {
            driverNameById.put(toLong(r.get("id")), asString(r.get("driver_name")));
        }
        // All driver ids are needed to fetch their devices; we crop AFTER
        // computing volumes so the top-N drivers reflect real activity, not
        // just the ones with the most devices listed.
        List<Long> allDriverIds = new ArrayList<>(driverNameById.keySet());

        List<Map<String, Object>> deviceRows = dashboardMapper.topologyDevicesByDrivers(tenantId, allDriverIds);
        long deviceTotal = deviceRows.size();
        Map<Long, Long> deviceToDriver = new HashMap<>(deviceRows.size() * 2);
        Map<Long, String> deviceNameById = new LinkedHashMap<>();
        for (Map<String, Object> r : deviceRows) {
            Long did = toLong(r.get("id"));
            deviceToDriver.put(did, toLong(r.get("driver_id")));
            deviceNameById.put(did, asString(r.get("device_name")));
        }
        List<Long> allDeviceIds = new ArrayList<>(deviceNameById.keySet());

        List<Map<String, Object>> bindingRows = allDeviceIds.isEmpty()
                ? Collections.emptyList()
                : dashboardMapper.topologyProfileBindings(tenantId, allDeviceIds);
        // device → set<profile>  and  profile → set<device>
        Map<Long, Set<Long>> profilesByDevice = new HashMap<>();
        Map<Long, Set<Long>> devicesByProfile = new LinkedHashMap<>();
        for (Map<String, Object> r : bindingRows) {
            Long did = toLong(r.get("device_id"));
            Long pid = toLong(r.get("profile_id"));
            profilesByDevice.computeIfAbsent(did, k -> new HashSet<>()).add(pid);
            devicesByProfile.computeIfAbsent(pid, k -> new LinkedHashSet<>()).add(did);
        }

        Set<Long> allProfileIdSet = devicesByProfile.keySet();
        List<Map<String, Object>> profileRows = allProfileIdSet.isEmpty()
                ? Collections.emptyList()
                : dashboardMapper.topologyProfilesByIds(tenantId, allProfileIdSet);
        Map<Long, String> profileNameById = new LinkedHashMap<>();
        for (Map<String, Object> r : profileRows) {
            profileNameById.put(toLong(r.get("id")), asString(r.get("profile_name")));
        }

        List<Map<String, Object>> pointRows = allProfileIdSet.isEmpty()
                ? Collections.emptyList()
                : dashboardMapper.topologyPointsByProfiles(tenantId, allProfileIdSet);
        long pointTotal = pointRows.size();
        // profile → ordered list of points
        Map<Long, List<Map<String, Object>>> pointsByProfile = new LinkedHashMap<>();
        for (Map<String, Object> r : pointRows) {
            pointsByProfile.computeIfAbsent(toLong(r.get("profile_id")), k -> new ArrayList<>()).add(r);
        }

        // ---- Fetch volumes if needed -------------------------------------
        // Map<(deviceId, pointId), count>. Empty in cardinality mode.
        Map<Long, Map<Long, Long>> volumeByDevicePoint = new HashMap<>();
        if (volumeMode) {
            LocalDateTime from = fromOfRange(rangeKey);
            List<Map<String, Object>> volumeRows = dashboardMapper.topologyPointVolumes(tenantId, from);
            for (Map<String, Object> r : volumeRows) {
                Long did = toLong(r.get("device_id"));
                Long pid = toLong(r.get("point_id"));
                long cnt = toLong(r.get("cnt"));
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
            for (Map.Entry<Long, List<Map<String, Object>>> e : pointsByProfile.entrySet()) {
                Long profileId = e.getKey();
                Set<Long> devs = devicesByProfile.getOrDefault(profileId, Collections.emptySet());
                for (Map<String, Object> r : e.getValue()) {
                    Long pid = toLong(r.get("id"));
                    long sum = 0;
                    for (Long did : devs) {
                        Map<Long, Long> vs = volumeByDevicePoint.get(did);
                        if (vs != null) sum += vs.getOrDefault(pid, 0L);
                    }
                    pointWeight.put(pid, sum);
                }
            }
            for (Map.Entry<Long, Map<Long, Long>> e : volumeByDevicePoint.entrySet()) {
                Long did = e.getKey();
                long sum = 0;
                for (long v : e.getValue().values()) sum += v;
                deviceWeight.put(did, sum);
                Long drvId = deviceToDriver.get(did);
                if (drvId != null) driverWeight.merge(drvId, sum, Long::sum);
            }
        } else {
            // Cardinality fallbacks mirror the original Top-N behaviour:
            //   driver weight = count of devices
            //   device weight = count of profile bindings
            //   point weight = 1 (stable tie-break on id desc later)
            for (Map<String, Object> r : driverRows) {
                driverWeight.put(toLong(r.get("id")), toLong(r.get("device_count")));
            }
            for (Map<String, Object> r : deviceRows) {
                deviceWeight.put(toLong(r.get("id")), toLong(r.get("profile_count")));
            }
            for (Map<String, Object> r : pointRows) {
                pointWeight.put(toLong(r.get("id")), 1L);
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
        List<Map<String, Object>> activeDrivers = volumeMode
                ? driverRows.stream()
                  .filter(r -> nullZero(driverWeight.get(toLong(r.get("id")))) > 0)
                  .collect(java.util.stream.Collectors.toList())
                : new ArrayList<>(driverRows);

        // ---- Crop Top-N drivers ----------------------------------------
        activeDrivers.sort(cmpByMap(driverWeight, v -> toLong(v.get("id"))));
        List<Map<String, Object>> topDrivers = activeDrivers.size() > TOP_DRIVERS
                ? activeDrivers.subList(0, TOP_DRIVERS)
                : activeDrivers;
        Set<Long> topDriverIds = new LinkedHashSet<>();
        for (Map<String, Object> r : topDrivers) topDriverIds.add(toLong(r.get("id")));

        // ---- Crop Top-N devices (must belong to a kept driver) ----------
        List<Map<String, Object>> filteredDevices = new ArrayList<>();
        for (Map<String, Object> r : deviceRows) {
            if (!topDriverIds.contains(toLong(r.get("driver_id")))) continue;
            if (volumeMode && nullZero(deviceWeight.get(toLong(r.get("id")))) == 0) continue;
            filteredDevices.add(r);
        }
        filteredDevices.sort(cmpByMap(deviceWeight, v -> toLong(v.get("id"))));
        List<Map<String, Object>> topDevices = filteredDevices.size() > TOP_DEVICES
                ? filteredDevices.subList(0, TOP_DEVICES)
                : filteredDevices;
        Set<Long> topDeviceIdSet = new LinkedHashSet<>();
        for (Map<String, Object> r : topDevices) topDeviceIdSet.add(toLong(r.get("id")));

        // Per-driver "others:device:{driverId}" buckets for cropped
        // devices. In volume mode only devices that had activity but got
        // cropped land here — zero-flow devices were filtered above, so
        // Others never represents "nothing is happening".
        Map<Long, List<TopologyHiddenChildVO>> otherDevicesByDriver = new LinkedHashMap<>();
        for (Map<String, Object> r : filteredDevices) {
            Long deviceId = toLong(r.get("id"));
            if (topDeviceIdSet.contains(deviceId)) continue;
            Long drvId = toLong(r.get("driver_id"));
            TopologyHiddenChildVO hidden = new TopologyHiddenChildVO();
            hidden.setId("device:" + deviceId);
            hidden.setName(asString(r.get("device_name")));
            hidden.setType("device");
            otherDevicesByDriver.computeIfAbsent(drvId, k -> new ArrayList<>()).add(hidden);
        }

        // ---- Profile set (bound to kept devices) -----------------------
        // In volume mode drop profiles whose points collectively got no
        // samples — otherwise the profile column paints a tile with no
        // outgoing band, exactly the "空模板" case the operator flagged.
        Set<Long> keptProfileIds = new LinkedHashSet<>();
        for (Long did : topDeviceIdSet) {
            Set<Long> pids = profilesByDevice.get(did);
            if (pids != null) keptProfileIds.addAll(pids);
        }
        if (volumeMode) {
            Set<Long> active = new LinkedHashSet<>();
            for (Long pid : keptProfileIds) {
                long sum = 0;
                for (Map<String, Object> p : pointsByProfile.getOrDefault(pid, Collections.emptyList())) {
                    sum += nullZero(pointWeight.get(toLong(p.get("id"))));
                    if (sum > 0) break;
                }
                if (sum > 0) active.add(pid);
            }
            keptProfileIds = active;
        }

        // ---- Compose nodes + links -------------------------------------
        List<TopologyNodeVO> nodes = new ArrayList<>();
        List<TopologyLinkVO> links = new ArrayList<>();

        for (Map<String, Object> r : topDrivers) {
            Long id = toLong(r.get("id"));
            nodes.add(node("driver:" + id, driverNameById.get(id), 1, "driver", null));
        }

        // Driver → Device links. Edge weight in cardinality mode = 1 (one
        // relationship); in volume mode = that device's total pv count.
        for (Map<String, Object> r : topDevices) {
            Long id = toLong(r.get("id"));
            Long driverId = toLong(r.get("driver_id"));
            String name = asString(r.get("device_name"));
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
        for (Map<String, Object> r : bindingRows) {
            Long deviceId = toLong(r.get("device_id"));
            Long profileId = toLong(r.get("profile_id"));
            if (!topDeviceIdSet.contains(deviceId)) continue;
            if (!keptProfileIds.contains(profileId)) continue;
            long w = 1L;
            if (volumeMode) {
                long sum = 0;
                Map<Long, Long> devVols = volumeByDevicePoint.get(deviceId);
                List<Map<String, Object>> pts = pointsByProfile.getOrDefault(profileId, Collections.emptyList());
                if (devVols != null) {
                    for (Map<String, Object> p : pts) {
                        sum += devVols.getOrDefault(toLong(p.get("id")), 0L);
                    }
                }
                if (sum == 0) continue;
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
            List<Map<String, Object>> allPoints = new ArrayList<>(
                    pointsByProfile.getOrDefault(profileId, Collections.emptyList()));
            if (volumeMode) {
                allPoints.removeIf(p -> nullZero(pointWeight.get(toLong(p.get("id")))) == 0);
                allPoints.sort(cmpByMap(pointWeight, v -> toLong(v.get("id"))));
            } else {
                allPoints.sort(Comparator.comparingLong((Map<String, Object> r) -> toLong(r.get("id"))).reversed());
            }

            int keep = Math.min(TOP_POINTS_PER_PROFILE, allPoints.size());
            for (int i = 0; i < keep; i++) {
                Map<String, Object> r = allPoints.get(i);
                Long id = toLong(r.get("id"));
                String name = asString(r.get("point_name"));
                nodes.add(node("point:" + id, name, 4, "point", null));
                long w = volumeMode ? nullZero(pointWeight.get(id)) : 1L;
                links.add(link("profile:" + profileId, "point:" + id, Math.max(1L, w)));
            }

            if (allPoints.size() > keep) {
                List<TopologyHiddenChildVO> children = new ArrayList<>(allPoints.size() - keep);
                long sumW = 0;
                for (int i = keep; i < allPoints.size(); i++) {
                    Map<String, Object> r = allPoints.get(i);
                    Long id = toLong(r.get("id"));
                    TopologyHiddenChildVO hidden = new TopologyHiddenChildVO();
                    hidden.setId("point:" + id);
                    hidden.setName(asString(r.get("point_name")));
                    hidden.setType("point");
                    children.add(hidden);
                    if (volumeMode) sumW += nullZero(pointWeight.get(id));
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
