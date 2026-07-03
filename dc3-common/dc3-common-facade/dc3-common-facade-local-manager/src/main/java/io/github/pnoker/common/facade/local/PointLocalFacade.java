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
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.facade.local.builder.FacadePointBuilder;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * In-process PointFacade implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointLocalFacade implements PointFacade {

    private final PointService pointService;

    private final FacadePointBuilder facadePointBuilder;

    @Override
    public FacadePointBO getById(Long tenantId, Long id) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            PointBO managerBO = pointService.getById(id);
            return Objects.isNull(managerBO) ? null : facadePointBuilder.toFacadeBO(managerBO);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<FacadePointBO> listByIds(Long tenantId, Collection<Long> ids) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            if (Objects.isNull(ids) || ids.isEmpty()) {
                return Collections.emptyList();
            }
            List<PointBO> list = pointService.listByIds(new HashSet<>(ids));
            if (Objects.isNull(list) || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(facadePointBuilder::toFacadeBO).toList();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public FacadePage<FacadePointBO> listByPage(FacadePointQuery query) {
        TenantContextHolder.setTenantId(query.getTenantId());
        try {
            PointQuery managerQuery = facadePointBuilder.toManagerQuery(query);
            Page<PointBO> page = pointService.list(managerQuery);
            if (Objects.isNull(page)) {
                return FacadePage.empty();
            }

            List<FacadePointBO> records = page.getRecords().stream().map(facadePointBuilder::toFacadeBO).toList();
            return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
        } finally {
            TenantContextHolder.clear();
        }
    }

}
