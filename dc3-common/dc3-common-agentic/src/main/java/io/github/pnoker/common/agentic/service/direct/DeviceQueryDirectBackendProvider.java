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
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 * Builds deterministic tenant-scoped device answers for device-query requests.
 *
 * @author pnoker
 * @version 2026.5.16
 * @since 2022.1.0
 */
@Component
public class DeviceQueryDirectBackendProvider {

    private final DeviceFacade deviceFacade;

    public DeviceQueryDirectBackendProvider(DeviceFacade deviceFacade) {
        this.deviceFacade = deviceFacade;
    }

    public DirectBackendResult build(RequestHeader.UserHeader userHeader,
                                     Queue<AgenticRequestContext.ToolEvent> toolEvents) {
        offerToolEvent(toolEvents, "searchDevices", "manager", "Load tenant device snapshot");
        FacadeDeviceQuery query = new FacadeDeviceQuery();
        query.setTenantId(userHeader.getTenantId());
        query.setPage(page(1, 50));
        FacadePage<FacadeDeviceBO> page = deviceFacade.selectByPage(query);
        if (Objects.isNull(page) || Objects.isNull(page.getRecords()) || page.getRecords().isEmpty()) {
            return DirectBackendResult.direct(DirectAnswer.message("设备查询结果", "当前租户下没有查询到设备。"));
        }
        List<List<String>> rows = page.getRecords().stream()
                .limit(50)
                .map(device -> List.of(
                        String.valueOf(device.getId()),
                        Objects.toString(device.getDeviceName(), ""),
                        Objects.toString(device.getDeviceCode(), ""),
                        Objects.toString(device.getDriverId(), ""),
                        Objects.toString(device.getEnableFlag(), ""),
                        Objects.toString(device.getProfileIds(), "")
                ))
                .toList();
        return DirectBackendResult.direct(DirectAnswer.table("设备查询结果", null,
                List.of(
                        new DirectAnswer.Field("页码", page.getCurrent() + "/" + page.getPages()),
                        new DirectAnswer.Field("总数", String.valueOf(page.getTotal()))
                ),
                List.of(new DirectAnswer.Table("设备列表", List.of("ID", "Name", "Code", "Driver ID", "Enabled",
                        "Profiles"), rows)),
                List.of()));
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

}
