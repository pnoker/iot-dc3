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

package io.github.pnoker.common.facade.local;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDeviceBuilder;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.manager.service.DeviceService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-process implementation: routes each call straight into {@link DeviceService}.
 * <p>
 * Selected when {@code dc3.facade.mode=local}. Carries zero serialization cost — the same
 * JVM handles both caller and service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Slf4j
@Component
public class DeviceLocalFacade implements DeviceFacade {

    @Resource
    private DeviceService deviceService;

    @Resource
    private FacadeDeviceBuilder facadeDeviceBuilder;

    @Override
    public FacadeDeviceBO selectById(Long id) {
        DeviceBO managerBO = deviceService.selectById(id);
        return Objects.isNull(managerBO) ? null : facadeDeviceBuilder.toFacadeBO(managerBO);
    }

    @Override
    public FacadePage<FacadeDeviceBO> selectByPage(FacadeDeviceQuery query) {
        DeviceQuery managerQuery = facadeDeviceBuilder.toManagerQuery(query);
        Page<DeviceBO> page = deviceService.selectByPage(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadeDeviceBO> records = page.getRecords().stream().map(facadeDeviceBuilder::toFacadeBO).toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

    @Override
    public List<FacadeDeviceBO> selectByProfileId(Long profileId) {
        List<DeviceBO> list = deviceService.selectByProfileId(profileId);
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeDeviceBuilder::toFacadeBO).toList();
    }

    @Override
    public List<FacadeDeviceBO> selectByDriverId(Long driverId) {
        List<DeviceBO> list = deviceService.selectByDriverId(driverId);
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeDeviceBuilder::toFacadeBO).toList();
    }

}
