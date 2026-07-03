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
import io.github.pnoker.common.facade.api.ProfileFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import io.github.pnoker.common.facade.local.builder.FacadeProfileBuilder;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.manager.service.ProfileService;
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
 * In-process ProfileFacade implementation.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileLocalFacade implements ProfileFacade {

    private final ProfileService profileService;

    private final FacadeProfileBuilder facadeProfileBuilder;

    @Override
    public FacadeProfileBO getById(Long tenantId, Long id) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            ProfileBO managerBO = profileService.getById(id);
            return Objects.isNull(managerBO) ? null : facadeProfileBuilder.toFacadeBO(managerBO);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<FacadeProfileBO> listByIds(Long tenantId, Collection<Long> ids) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            if (Objects.isNull(ids) || ids.isEmpty()) {
                return Collections.emptyList();
            }
            List<ProfileBO> list = profileService.listByIds(new HashSet<>(ids));
            if (Objects.isNull(list) || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(facadeProfileBuilder::toFacadeBO).toList();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public FacadePage<FacadeProfileBO> listByPage(FacadeProfileQuery query) {
        TenantContextHolder.setTenantId(query.getTenantId());
        try {
            ProfileQuery managerQuery = facadeProfileBuilder.toManagerQuery(query);
            Page<ProfileBO> page = profileService.list(managerQuery);
            if (Objects.isNull(page)) {
                return FacadePage.empty();
            }

            List<FacadeProfileBO> records = page.getRecords().stream().map(facadeProfileBuilder::toFacadeBO).toList();
            return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Override
    public List<FacadeProfileBO> listByDeviceId(Long tenantId, Long deviceId) {
        TenantContextHolder.setTenantId(tenantId);
        try {
            List<ProfileBO> list = profileService.listByDeviceId(deviceId);
            if (Objects.isNull(list) || list.isEmpty()) {
                return Collections.emptyList();
            }
            return list.stream().map(facadeProfileBuilder::toFacadeBO).toList();
        } finally {
            TenantContextHolder.clear();
        }
    }

}
