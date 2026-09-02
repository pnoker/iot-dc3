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

import io.github.pnoker.common.manager.entity.bo.LabelBindBO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

public interface ReactiveLabelBindStore {
    Mono<OffsetPage<LabelBindBO>> list(BindingFilter filter);

    Mono<LabelBindBO> get(Long tenantId, Long id);

    Mono<LabelBindBO> getByEntity(Long tenantId, byte type, Long labelId, Long entityId);

    Mono<LabelBindBO> insert(LabelBindBO value);

    Mono<LabelBindBO> update(LabelBindBO value);

    Mono<Boolean> delete(Long tenantId, Long id, Long operatorId, String operatorName);
}
