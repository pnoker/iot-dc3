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

import io.github.pnoker.common.manager.entity.bo.LabelBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Reactive persistence port for tenant-scoped labels. */
public interface ReactiveLabelStore {
    /** Page labels matching the tenant-scoped filters. */
    Mono<OffsetPage<LabelBO>> list(LabelFilter filter);

    /** Load the label scoped to the tenant by id. */
    Mono<LabelBO> get(Long tenantId, Long id);

    /** Resolve the label by its name. */
    Mono<LabelBO> getByName(Long tenantId, String name, byte entityType);

    /** Report whether the label has active bindings. */
    Mono<Boolean> hasActiveBindings(Long tenantId, Long labelId);

    /** Insert one label and emit the stored row. */
    Mono<LabelBO> insert(LabelBO label);

    /** Update one label and emit the updated row. */
    Mono<LabelBO> update(LabelBO label);

    /** Delete the label, reporting whether a row was removed. */
    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
