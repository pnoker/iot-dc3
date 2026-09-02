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
package io.github.pnoker.common.manager.biz;

import io.github.pnoker.common.entity.option.DictionaryOption;
import io.github.pnoker.common.manager.entity.query.DictionaryListRequest;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/** Read-only option query service for manager entities. */
public interface DictionaryForManagerService {

    Mono<OffsetPage<DictionaryOption>> listDriverOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listProfileOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listProfilePointOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDevicePointOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDeviceOptions(Long tenantId, DictionaryListRequest request);

    Mono<OffsetPage<DictionaryOption>> listDriverDeviceOptions(Long tenantId, DictionaryListRequest request);
}
