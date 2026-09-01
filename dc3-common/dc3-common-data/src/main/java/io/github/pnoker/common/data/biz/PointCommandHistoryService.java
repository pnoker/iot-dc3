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

package io.github.pnoker.common.data.biz;

import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import reactor.core.publisher.Mono;

/**
 * Business service for point command history queries.
 *
 * @author pnoker
 * @since 2026.5.23
 */
public interface PointCommandHistoryService {

    /**
     * Query a single command history by its commandId.
     *
     * @param commandId unique command identifier
     * @return matching command history row, or null
     */
    Mono<PointCommandHistoryVO> getByCommandId(Long tenantId, String commandId);

    /**
     * Return by command identifier.
     *
     * @param commandId command identifier
     * @return get by command identifier result
     */
    default Mono<PointCommandHistoryVO> getByCommandId(String commandId) {
        return getByCommandId(null, commandId);
    }

    /**
     * Query point command histories with pagination and optional filters.
     *
     * @param tenantId current tenant id
     * @param queryVO  query filters
     * @return paginated command history rows
     */
    Mono<OffsetPage<PointCommandHistoryVO>> list(Long tenantId, PointCommandHistoryQueryVO queryVO);

}
