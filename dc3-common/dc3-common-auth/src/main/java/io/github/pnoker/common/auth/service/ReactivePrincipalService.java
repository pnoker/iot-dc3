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
package io.github.pnoker.common.auth.service;

import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.repository.PrincipalFilter;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.Collection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Reactive application service for the global principal catalog. */
public interface ReactivePrincipalService {

    Mono<PrincipalBO> getById(Long tenantId, Long id);

    Mono<OffsetPage<PrincipalBO>> list(Long tenantId, PrincipalFilter filter);

    Flux<PrincipalBO> listByIds(Long tenantId, Collection<Long> ids);

    Mono<PrincipalBO> setEnableFlag(
            Long tenantId, Long id, EnableFlagEnum target, Long operatorId, String operatorName);

    Mono<Boolean> touchLastLogin(Long id);
}
