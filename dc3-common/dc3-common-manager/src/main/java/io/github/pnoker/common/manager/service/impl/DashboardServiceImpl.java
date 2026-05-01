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

import io.github.pnoker.common.enums.DriverTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.manager.entity.vo.dashboard.BucketVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DeviceStatsVO;
import io.github.pnoker.common.manager.entity.vo.dashboard.DriverStatsVO;
import io.github.pnoker.common.manager.mapper.DashboardMapper;
import io.github.pnoker.common.manager.service.DashboardService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author pnoker
 * @since 2026.5.2
 */
@Slf4j
@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private DashboardMapper dashboardMapper;

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

        out.setByEnable(byEnable);
        out.setByType(byType);
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

    // ---- internal helpers -----------------------------------------------

    @FunctionalInterface
    private interface KeyFormatter {
        String format(Object raw);
    }

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

    private static String driverTypeKey(Object raw) {
        if (raw == null) return "UNKNOWN";
        Byte b = raw instanceof Number n ? n.byteValue() : Byte.parseByte(raw.toString());
        DriverTypeFlagEnum e = DriverTypeFlagEnum.ofIndex(b);
        return e == null ? "UNKNOWN" : e.name();
    }
}
