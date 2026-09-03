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
package io.github.pnoker.common.data.service;

import io.github.pnoker.common.data.entity.bo.RuleStateBO;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive rule runtime state service. */
public interface RuleStateService {

    /** Resolve the rule state by its id. */
    Mono<RuleStateBO> getById(Long tenantId, Long id);

    /** Page rule states matching the tenant-scoped filters. */
    Mono<OffsetPage<RuleStateBO>> list(Long tenantId, RuleStateQuery query);

    /** Delete the rule state, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id);
}
