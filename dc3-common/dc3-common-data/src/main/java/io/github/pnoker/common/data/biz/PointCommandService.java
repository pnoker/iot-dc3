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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.model.PointCommandDO;
import io.github.pnoker.common.data.entity.vo.PointCommandQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;

/**
 * Business service for point command operations.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2016.10.1
 */
public interface PointCommandService {

    /**
     * Submit a read command.
     *
     * @param tenantId current tenant id
     * @param entityVO PointCommandReadVO
     * @return generated commandId for status tracking
     */
    String read(Long tenantId, PointCommandReadVO entityVO);

    /**
     * Read command from trusted internal callers.
     *
     * @param entityVO PointCommandReadVO
     * @return generated commandId
     */
    default String read(PointCommandReadVO entityVO) {
        return read(null, entityVO);
    }

    /**
     * Submit a write command.
     *
     * @param tenantId current tenant id
     * @param entityVO PointCommandWriteVO
     * @return generated commandId for status tracking
     */
    String write(Long tenantId, PointCommandWriteVO entityVO);

    /**
     * Write command from trusted internal callers.
     *
     * @param entityVO PointCommandWriteVO
     * @return generated commandId
     */
    default String write(PointCommandWriteVO entityVO) {
        return write(null, entityVO);
    }

    /**
     * Query a single command by its commandId.
     *
     * @param commandId unique command identifier
     * @return matching command row, or null
     */
    PointCommandDO getByCommandId(String commandId);

    /**
     * Query commands with pagination and optional filters.
     *
     * @param tenantId current tenant id
     * @param queryVO  query filters (deviceId, pointId, status, type, page, size)
     * @return paginated command rows
     */
    Page<PointCommandDO> list(Long tenantId, PointCommandQueryVO queryVO);

}
