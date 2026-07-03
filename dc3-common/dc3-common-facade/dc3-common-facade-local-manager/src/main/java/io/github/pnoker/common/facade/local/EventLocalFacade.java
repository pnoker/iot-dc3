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
import io.github.pnoker.common.facade.api.EventFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import io.github.pnoker.common.facade.local.builder.FacadeEventBuilder;
import io.github.pnoker.common.manager.entity.bo.EventBO;
import io.github.pnoker.common.manager.entity.query.EventQuery;
import io.github.pnoker.common.manager.service.EventService;
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
 * In-process EventFacade implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventLocalFacade implements EventFacade {

    private final EventService eventService;

    private final FacadeEventBuilder facadeEventBuilder;

    @Override
    public FacadeEventBO getById(Long tenantId, Long id) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            EventBO managerBO = eventService.getById(id);
            return Objects.isNull(managerBO) ? null : facadeEventBuilder.toFacadeBO(managerBO);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<FacadeEventBO> listByIds(Long tenantId, Collection<Long> ids) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            if (Objects.isNull(ids) || ids.isEmpty()) {
                return Collections.emptyList();
            }
            List<EventBO> list = eventService.listByIds(new HashSet<>(ids));
            if (Objects.isNull(list) || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(facadeEventBuilder::toFacadeBO).toList();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public FacadePage<FacadeEventBO> listByPage(FacadeEventQuery query) {
        TenantContextHolder.setTenantId(query.getTenantId());
        try {
            EventQuery managerQuery = facadeEventBuilder.toManagerQuery(query);
            Page<EventBO> page = eventService.list(managerQuery);
            if (Objects.isNull(page)) {
                return FacadePage.empty();
            }

            List<FacadeEventBO> records = page.getRecords().stream().map(facadeEventBuilder::toFacadeBO).toList();
            return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
        } finally {
            TenantContextHolder.clear();
        }
    }

}
