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

import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Protocol-neutral profile/template facade.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
public interface ProfileFacade {

    private static boolean matchesTenant(Long tenantId, FacadeProfileBO profile) {
        return Objects.nonNull(profile) && Objects.equals(tenantId, profile.getTenantId());
    }

    FacadeProfileBO selectById(Long id);

    default FacadeProfileBO selectById(Long tenantId, Long id) {
        if (Objects.isNull(tenantId)) {
            return null;
        }
        FacadeProfileBO profile = selectById(id);
        return matchesTenant(tenantId, profile) ? profile : null;
    }

    List<FacadeProfileBO> selectByIds(Collection<Long> ids);

    default List<FacadeProfileBO> selectByIds(Long tenantId, Collection<Long> ids) {
        if (Objects.isNull(tenantId) || Objects.isNull(ids) || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return selectByIds(ids).stream()
                .filter(profile -> matchesTenant(tenantId, profile))
                .toList();
    }

    FacadePage<FacadeProfileBO> selectByPage(FacadeProfileQuery query);

    List<FacadeProfileBO> selectByDeviceId(Long deviceId);

    default List<FacadeProfileBO> selectByDeviceId(Long tenantId, Long deviceId) {
        if (Objects.isNull(tenantId)) {
            return Collections.emptyList();
        }
        return selectByDeviceId(deviceId).stream()
                .filter(profile -> matchesTenant(tenantId, profile))
                .toList();
    }

}
