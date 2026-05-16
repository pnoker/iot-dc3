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
package io.github.pnoker.common.agentic.service.direct;

import io.github.pnoker.common.agentic.context.AgenticRequestContext;
import io.github.pnoker.common.agentic.entity.request.DirectQueryRequest;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

/**
 * Resolves deterministic data-monitor requests directly through the DC3 facade layer.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Component
public class DataMonitorDirectBackendProvider {

    private final DeviceFacade deviceFacade;

    private final DriverFacade driverFacade;

    private final PointFacade pointFacade;

    private final PointValueFacade pointValueFacade;

    public DataMonitorDirectBackendProvider(DeviceFacade deviceFacade, DriverFacade driverFacade,
                                            PointFacade pointFacade, PointValueFacade pointValueFacade) {
        this.deviceFacade = deviceFacade;
        this.driverFacade = driverFacade;
        this.pointFacade = pointFacade;
        this.pointValueFacade = pointValueFacade;
    }

    public DirectBackendResult build(DirectQueryRequest directQuery, RequestHeader.UserHeader userHeader,
                                     Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        if (Objects.nonNull(directQuery)) {
            if (directQuery.isPointValueQuery()) {
                return buildResolvedPointValueResult(directQuery, userHeader, toolEvents);
            }
            return DirectBackendResult.direct(DirectAnswer.message("查询失败",
                    "不支持的确定性查询类型：" + StringUtils.defaultString(directQuery.getType())));
        }

        offerToolEvent(toolEvents, "getMonitoringSnapshot", "agentic", "Load monitoring snapshot");
        FacadeDeviceQuery deviceQuery = new FacadeDeviceQuery();
        deviceQuery.setTenantId(userHeader.getTenantId());
        deviceQuery.setPage(page(1, 10));
        FacadePage<FacadeDeviceBO> devices = deviceFacade.selectByPage(deviceQuery);
        FacadeDriverQuery driverQuery = new FacadeDriverQuery();
        driverQuery.setTenantId(userHeader.getTenantId());
        driverQuery.setPage(page(1, 10));
        FacadePage<FacadeDriverBO> drivers = driverFacade.selectByPage(driverQuery);
        FacadePointQuery pointQuery = new FacadePointQuery();
        pointQuery.setTenantId(userHeader.getTenantId());
        pointQuery.setPage(page(1, 10));
        FacadePage<FacadePointBO> points = pointFacade.selectByPage(pointQuery);
        String context = "Monitoring snapshot:\n"
                + "- devices total: " + total(devices) + "\n"
                + "- drivers total: " + total(drivers) + "\n"
                + "- points total: " + total(points) + "\n"
                + "Sample devices: " + sampleDeviceNames(devices);
        return DirectBackendResult.contextOnly(context);
    }

    public boolean isResolvedPointValueRequest(DirectQueryRequest directQuery) {
        return Objects.nonNull(directQuery) && directQuery.isPointValueQuery();
    }

    public DirectBackendResult failedQueryResult() {
        return DirectBackendResult.direct(DirectAnswer.message("查询失败",
                "后端数据查询执行失败，请稍后重试或检查查询条件。"));
    }

    private DirectBackendResult buildResolvedPointValueResult(DirectQueryRequest directQuery,
                                                              RequestHeader.UserHeader userHeader,
                                                              Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        if (!directQuery.hasDeviceSelector() || !directQuery.hasPointSelector()) {
            return DirectBackendResult.direct(DirectAnswer.message("查询失败",
                    "确定性位号查询需要明确的设备选择器和位号选择器。请通过 directQuery.deviceId/deviceName/deviceCode "
                            + "与 directQuery.pointId/pointName/pointCode 传入结构化参数。"));
        }

        Long tenantId = userHeader.getTenantId();
        FacadeDeviceBO device = resolveDevice(tenantId, directQuery);
        if (Objects.isNull(device)) {
            offerToolEvent(toolEvents, "searchDevices", "manager",
                    "Device lookup returned no unique match for " + deviceSelectorText(directQuery));
            return DirectBackendResult.direct(DirectAnswer.message("查询失败",
                    "当前租户下没有找到唯一匹配的设备 " + deviceSelectorText(directQuery)
                            + "。请检查设备 ID、名称或编码后重试。"));
        }
        offerToolEvent(toolEvents, "searchDevices", "manager",
                "Resolved device " + device.getDeviceName() + " (" + device.getId() + ")");

        FacadePointBO point = resolvePoint(tenantId, device.getId(), directQuery);
        if (Objects.isNull(point)) {
            offerToolEvent(toolEvents, "searchPoints", "manager",
                    "Point lookup returned no unique match for " + pointSelectorText(directQuery));
            return DirectBackendResult.direct(DirectAnswer.table("查询失败",
                    "已找到设备 " + device.getDeviceName() + "，但没有找到唯一匹配的位号 "
                            + pointSelectorText(directQuery) + "。",
                    List.of(
                            new DirectAnswer.Field("设备名称", device.getDeviceName()),
                            new DirectAnswer.Field("设备编码", device.getDeviceCode()),
                            new DirectAnswer.Field("设备ID", String.valueOf(device.getId()))
                    ),
                    List.of(),
                    List.of()));
        }
        offerToolEvent(toolEvents, "searchPoints", "manager",
                "Resolved point " + point.getPointName() + " (" + point.getId() + ")");

        FacadePointValueBO latestValue = pointValueFacade.lastValue(tenantId, device.getId(), point.getId());
        offerToolEvent(toolEvents, "getLatestPointValue", "data", "Loaded latest point value");

        List<String> history = List.of();
        int count = directQuery.normalizedLimit();
        if (count > 1) {
            history = pointValueFacade.history(tenantId, device.getId(), point.getId(), count);
            offerToolEvent(toolEvents, "getPointValueHistory", "data",
                    "Loaded latest " + count + " history values");
        }

        List<DirectAnswer.Field> fields = new ArrayList<>();
        fields.add(new DirectAnswer.Field("设备",
                device.getDeviceName() + " (id=" + device.getId() + ", code=" + device.getDeviceCode() + ")"));
        fields.add(new DirectAnswer.Field("位号",
                point.getPointName() + " (id=" + point.getId() + ", code=" + point.getPointCode() + ")"));
        fields.add(new DirectAnswer.Field("单位", point.getUnit()));
        fields.add(new DirectAnswer.Field("数据类型", String.valueOf(point.getPointTypeFlag())));
        fields.add(new DirectAnswer.Field("读写标识", String.valueOf(point.getRwFlag())));
        if (Objects.nonNull(latestValue)) {
            fields.add(new DirectAnswer.Field("最新值", latestValue.getValue() + " (raw="
                    + latestValue.getRawValue() + ", time=" + latestValue.getCreateTime() + ")"));
        } else {
            fields.add(new DirectAnswer.Field("最新值", "未查询到最新值"));
        }

        List<DirectAnswer.Table> tables = new ArrayList<>();
        List<DirectAnswer.Chart> charts = new ArrayList<>();
        String message = null;
        if (!history.isEmpty()) {
            String title = "最新 " + history.size() + " 条历史值";
            if (history.size() == count) {
                title += "（后端返回顺序：新到旧）";
            }
            List<List<String>> rows = new ArrayList<>();
            for (int i = 0; i < history.size(); i++) {
                rows.add(List.of(String.valueOf(i + 1), history.get(i), StringUtils.defaultString(point.getUnit())));
            }
            tables.add(new DirectAnswer.Table(title, List.of("#", "值", "单位"), rows));
            DirectAnswer.Chart chart = buildHistoryChart(device.getDeviceName(), point.getPointName(), point.getUnit(),
                    history);
            if (Objects.nonNull(chart)) {
                charts.add(chart);
            }
        } else if (count > 1) {
            message = "后端没有返回请求的 " + count + " 条历史值。";
        }

        return DirectBackendResult.direct(DirectAnswer.table("位号数据查询结果", message, fields, tables, charts));
    }

    private FacadeDeviceBO resolveDevice(Long tenantId, DirectQueryRequest directQuery) {
        if (Objects.nonNull(directQuery.getDeviceId())) {
            FacadeDeviceBO device = deviceFacade.selectById(tenantId, directQuery.getDeviceId());
            return deviceMatchesSelector(device, directQuery) ? device : null;
        }

        List<FacadeDeviceBO> candidates = new ArrayList<>();
        if (StringUtils.isNotBlank(directQuery.getDeviceName())) {
            FacadeDeviceQuery nameQuery = new FacadeDeviceQuery();
            nameQuery.setTenantId(tenantId);
            nameQuery.setDeviceName(StringUtils.trim(directQuery.getDeviceName()));
            nameQuery.setPage(page(1, 10));
            addRecords(candidates, deviceFacade.selectByPage(nameQuery));
        }
        if (StringUtils.isNotBlank(directQuery.getDeviceCode())) {
            FacadeDeviceQuery codeQuery = new FacadeDeviceQuery();
            codeQuery.setTenantId(tenantId);
            codeQuery.setDeviceCode(StringUtils.trim(directQuery.getDeviceCode()));
            codeQuery.setPage(page(1, 10));
            addRecords(candidates, deviceFacade.selectByPage(codeQuery));
        }

        return uniqueDeviceMatch(candidates, directQuery);
    }

    private FacadePointBO resolvePoint(Long tenantId, Long deviceId, DirectQueryRequest directQuery) {
        if (Objects.nonNull(directQuery.getPointId())) {
            FacadePointBO point = pointFacade.selectById(tenantId, directQuery.getPointId());
            return pointMatchesSelector(point, directQuery) ? point : null;
        }

        List<FacadePointBO> candidates = new ArrayList<>();
        if (StringUtils.isNotBlank(directQuery.getPointName())) {
            FacadePointQuery nameQuery = new FacadePointQuery();
            nameQuery.setTenantId(tenantId);
            nameQuery.setDeviceId(deviceId);
            nameQuery.setPointName(StringUtils.trim(directQuery.getPointName()));
            nameQuery.setPage(page(1, 10));
            addRecords(candidates, pointFacade.selectByPage(nameQuery));
        }
        if (StringUtils.isNotBlank(directQuery.getPointCode())) {
            FacadePointQuery codeQuery = new FacadePointQuery();
            codeQuery.setTenantId(tenantId);
            codeQuery.setDeviceId(deviceId);
            codeQuery.setPointCode(StringUtils.trim(directQuery.getPointCode()));
            codeQuery.setPage(page(1, 10));
            addRecords(candidates, pointFacade.selectByPage(codeQuery));
        }

        return uniquePointMatch(candidates, directQuery);
    }

    private <T> void addRecords(List<T> target, FacadePage<T> page) {
        if (Objects.nonNull(page) && Objects.nonNull(page.getRecords())) {
            target.addAll(page.getRecords());
        }
    }

    private FacadeDeviceBO uniqueDeviceMatch(List<FacadeDeviceBO> candidates, DirectQueryRequest directQuery) {
        List<FacadeDeviceBO> matches = candidates.stream()
                .filter(Objects::nonNull)
                .filter(device -> deviceMatchesSelector(device, directQuery))
                .toList();
        return uniqueById(matches);
    }

    private FacadePointBO uniquePointMatch(List<FacadePointBO> candidates, DirectQueryRequest directQuery) {
        List<FacadePointBO> matches = candidates.stream()
                .filter(Objects::nonNull)
                .filter(point -> pointMatchesSelector(point, directQuery))
                .toList();
        return uniqueById(matches);
    }

    private boolean deviceMatchesSelector(FacadeDeviceBO device, DirectQueryRequest directQuery) {
        if (Objects.isNull(device)) {
            return false;
        }
        return (Objects.isNull(directQuery.getDeviceId()) || Objects.equals(device.getId(), directQuery.getDeviceId()))
                && (StringUtils.isBlank(directQuery.getDeviceName())
                        || equalsNormalized(device.getDeviceName(), directQuery.getDeviceName()))
                && (StringUtils.isBlank(directQuery.getDeviceCode())
                        || equalsNormalized(device.getDeviceCode(), directQuery.getDeviceCode()));
    }

    private boolean pointMatchesSelector(FacadePointBO point, DirectQueryRequest directQuery) {
        if (Objects.isNull(point)) {
            return false;
        }
        return (Objects.isNull(directQuery.getPointId()) || Objects.equals(point.getId(), directQuery.getPointId()))
                && (StringUtils.isBlank(directQuery.getPointName())
                        || equalsNormalized(point.getPointName(), directQuery.getPointName()))
                && (StringUtils.isBlank(directQuery.getPointCode())
                        || equalsNormalized(point.getPointCode(), directQuery.getPointCode()));
    }

    private boolean equalsNormalized(String value, String expected) {
        return StringUtils.isNotBlank(value) && StringUtils.isNotBlank(expected)
                && value.trim().equalsIgnoreCase(expected.trim());
    }

    private <T extends BaseBO> T uniqueById(List<T> candidates) {
        Map<Long, T> unique = new LinkedHashMap<>();
        for (T candidate : candidates) {
            if (Objects.nonNull(candidate.getId())) {
                unique.putIfAbsent(candidate.getId(), candidate);
            }
        }
        return unique.size() == 1 ? unique.values().iterator().next() : null;
    }

    private String deviceSelectorText(DirectQueryRequest directQuery) {
        if (Objects.nonNull(directQuery.getDeviceId())) {
            return "deviceId=" + directQuery.getDeviceId();
        }
        if (StringUtils.isNotBlank(directQuery.getDeviceName())) {
            return "deviceName=" + directQuery.getDeviceName();
        }
        return "deviceCode=" + directQuery.getDeviceCode();
    }

    private String pointSelectorText(DirectQueryRequest directQuery) {
        if (Objects.nonNull(directQuery.getPointId())) {
            return "pointId=" + directQuery.getPointId();
        }
        if (StringUtils.isNotBlank(directQuery.getPointName())) {
            return "pointName=" + directQuery.getPointName();
        }
        return "pointCode=" + directQuery.getPointCode();
    }

    private DirectAnswer.Chart buildHistoryChart(String deviceName, String pointName, String unit,
                                                 List<String> history) {
        List<List<Number>> dataPoints = new ArrayList<>();
        int rendered = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            try {
                double value = Double.parseDouble(StringUtils.trimToEmpty(history.get(i)));
                dataPoints.add(List.of(rendered, value));
                rendered++;
            } catch (NumberFormatException ignored) {
                // Non-numeric point values are still shown in the table; only chart data skips them.
            }
        }
        if (rendered == 0) {
            return null;
        }
        return new DirectAnswer.Chart("line", deviceName + " / " + pointName, unit, "index (oldest to newest)",
                "linear", List.of(new DirectAnswer.Series("value", dataPoints)));
    }

    private void offerToolEvent(Queue<AgenticRequestContext.ToolEvent> toolEvents, String toolName, String domain,
                                String description) {
        if (Objects.nonNull(toolEvents)) {
            toolEvents.offer(new AgenticRequestContext.ToolEvent(toolName, domain, description,
                    Instant.now().toEpochMilli()));
        }
    }

    private Pages page(long current, long size) {
        Pages page = new Pages();
        page.setCurrent(current);
        page.setSize(size);
        return page;
    }

    private long total(FacadePage<?> page) {
        return Objects.isNull(page) ? 0 : page.getTotal();
    }

    private String sampleDeviceNames(FacadePage<FacadeDeviceBO> page) {
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return "none";
        }
        return page.getRecords().stream()
                .limit(5)
                .map(FacadeDeviceBO::getDeviceName)
                .filter(StringUtils::isNotBlank)
                .toList()
                .toString();
    }

}
