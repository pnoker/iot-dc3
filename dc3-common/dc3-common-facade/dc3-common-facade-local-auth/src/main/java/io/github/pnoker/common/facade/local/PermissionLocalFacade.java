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

import io.github.pnoker.common.auth.entity.bo.ResourceBO;
import io.github.pnoker.common.auth.service.RoleResourceBindService;
import io.github.pnoker.common.facade.api.PermissionFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-process {@link PermissionFacade}.
 *
 * @author pnoker
 * @version 2026.6.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionLocalFacade implements PermissionFacade {

    private final RoleResourceBindService roleResourceBindService;

    @Override
    public Set<String> listPermissionCodes(Long tenantId, Long principalId) {
        if (tenantId == null || principalId == null) {
            return Set.of();
        }
        return roleResourceBindService.listResourceByPrincipalId(principalId, tenantId)
                .stream()
                .map(ResourceBO::getResourceCode)
                .filter(Objects::nonNull)
                .filter(code -> !code.isBlank())
                .collect(Collectors.toSet());
    }

}
