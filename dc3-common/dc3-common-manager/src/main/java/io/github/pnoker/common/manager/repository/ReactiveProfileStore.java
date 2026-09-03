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
package io.github.pnoker.common.manager.repository;

import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped profiles. */
public interface ReactiveProfileStore {

    /** Load the profile scoped to the tenant by id. */
    Mono<ProfileBO> get(Long tenantId, Long id);

    /** Check whether a record exists for the given name. */
    Mono<Boolean> existsByName(Long tenantId, String profileName, Long excludingId);

    /** Report whether the profile has associations. */
    Mono<Boolean> hasAssociations(Long tenantId, Long id);

    /** Insert one profile and emit the stored row. */
    Mono<ProfileBO> insert(ProfileBO value);

    /** Update one profile and emit the updated row. */
    Mono<ProfileBO> update(ProfileBO value, int expectedVersion);

    /** Delete the profile, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, int expectedVersion, Long operatorId, String operatorName);

    /** Resolve the profile by its name and type. */
    Mono<ProfileBO> getByNameAndType(Long tenantId, String name, ProfileTypeEnum type);

    /** List profiles matched by ids. */
    Flux<ProfileBO> listByIds(Long tenantId, List<Long> ids);

    /** List profiles matched by device id. */
    Flux<ProfileBO> listByDeviceId(Long tenantId, Long deviceId);

    /** Page profiles matching the tenant-scoped filters. */
    Mono<OffsetPage<ProfileBO>> list(ProfileFilter filter);
}
