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
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDriverBuilder;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.service.DriverService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * In-process DriverFacade implementation.
 *
 * @author pnoker
 * @since 2026.4.30
 */
@Slf4j
@Component
public class DriverLocalFacade implements DriverFacade {

    @Resource
    private DriverService driverService;

    @Resource
    private FacadeDriverBuilder facadeDriverBuilder;

    @Override
    public FacadeDriverBO selectById(Long id) {
        DriverBO managerBO = driverService.selectById(id);
        return Objects.isNull(managerBO) ? null : facadeDriverBuilder.toFacadeBO(managerBO);
    }

    @Override
    public FacadePage<FacadeDriverBO> selectByPage(FacadeDriverQuery query) {
        DriverQuery managerQuery = facadeDriverBuilder.toManagerQuery(query);
        Page<DriverBO> page = driverService.selectByPage(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadeDriverBO> records = page.getRecords().stream()
                .map(facadeDriverBuilder::toFacadeBO)
                .toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

    @Override
    public FacadeDriverBO selectByDeviceId(Long deviceId) {
        DriverBO managerBO = driverService.selectByDeviceId(deviceId);
        return Objects.isNull(managerBO) ? null : facadeDriverBuilder.toFacadeBO(managerBO);
    }
}
