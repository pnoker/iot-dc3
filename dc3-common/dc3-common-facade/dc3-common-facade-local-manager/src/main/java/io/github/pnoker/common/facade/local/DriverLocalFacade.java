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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * In-process DriverFacade implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DriverLocalFacade implements DriverFacade {

    private final DriverService driverService;

    private final FacadeDriverBuilder facadeDriverBuilder;

    @Override
    public FacadeDriverBO getById(Long id) {
        DriverBO managerBO = driverService.getById(id);
        return Objects.isNull(managerBO) ? null : facadeDriverBuilder.toFacadeBO(managerBO);
    }

    @Override
    public List<FacadeDriverBO> listByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<DriverBO> list = driverService.listByIds(new HashSet<>(ids));
        if (Objects.isNull(list) || list.isEmpty()) {
            return Collections.emptyList();
        }
        return list.stream().map(facadeDriverBuilder::toFacadeBO).toList();
    }

    @Override
    public FacadePage<FacadeDriverBO> listByPage(FacadeDriverQuery query) {
        DriverQuery managerQuery = facadeDriverBuilder.toManagerQuery(query);
        Page<DriverBO> page = driverService.list(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadeDriverBO> records = page.getRecords().stream().map(facadeDriverBuilder::toFacadeBO).toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }

    @Override
    public FacadeDriverBO getByDeviceId(Long deviceId) {
        DriverBO managerBO = driverService.getByDeviceId(deviceId, null);
        return Objects.isNull(managerBO) ? null : facadeDriverBuilder.toFacadeBO(managerBO);
    }

}
