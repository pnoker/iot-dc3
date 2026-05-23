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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Protocol-neutral command facade.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface CommandFacade {

    private static boolean matchesTenant(Long tenantId, FacadeCommandBO command) {
        return Objects.nonNull(command) && Objects.equals(tenantId, command.getTenantId());
    }

    FacadeCommandBO getById(Long id);

    default FacadeCommandBO getById(Long tenantId, Long id) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadeCommandBO command = getById(id);
        return matchesTenant(tenantId, command) ? command : null;
    }

    List<FacadeCommandBO> listByIds(Collection<Long> ids);

    default List<FacadeCommandBO> listByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(tenantId) || Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return listByIds(ids).stream()
                .filter(command -> matchesTenant(tenantId, command))
                .toList();
    }

    FacadePage<FacadeCommandBO> listByPage(FacadeCommandQuery query);

}
