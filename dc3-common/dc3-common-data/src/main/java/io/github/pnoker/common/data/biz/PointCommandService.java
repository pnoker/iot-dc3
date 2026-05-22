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

import io.github.pnoker.common.data.entity.model.PointCommandDO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;

/**
 * Business service for point command operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface PointCommandService {

    /**
     * Read command
     *
     * @param tenantId current tenant id
     * @param entityVO PointCommandReadVO
     */
    void read(Long tenantId, PointCommandReadVO entityVO);

    /**
     * Read command from trusted internal callers that do not carry request tenant
     * context.
     *
     * @param entityVO PointCommandReadVO
     */
    default void read(PointCommandReadVO entityVO) {
        read(null, entityVO);
    }

    /**
     * Write command
     *
     * @param tenantId current tenant id
     * @param entityVO PointCommandWriteVO
     */
    void write(Long tenantId, PointCommandWriteVO entityVO);

    /**
     * Write command from trusted internal callers that do not carry request tenant
     * context.
     *
     * @param entityVO PointCommandWriteVO
     */
    default void write(PointCommandWriteVO entityVO) {
        write(null, entityVO);
    }

    /**
     * Query a single command by its commandId.
     *
     * @param commandId unique command identifier
     * @return matching command row, or null
     */
    PointCommandDO getByCommandId(String commandId);

}
