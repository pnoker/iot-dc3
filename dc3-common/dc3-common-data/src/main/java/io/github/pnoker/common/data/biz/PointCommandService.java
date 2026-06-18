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

import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;

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
     * @param entityBO PointCommandReadBO
     * @return generated commandId for status tracking
     */
    String read(Long tenantId, PointCommandReadBO entityBO);

    /**
     * Read command from trusted internal callers.
     *
     * @param entityBO PointCommandReadBO
     * @return generated commandId
     */
    default String read(PointCommandReadBO entityBO) {
        return read(null, entityBO);
    }

    /**
     * Submit a write command.
     *
     * @param tenantId current tenant id
     * @param entityBO PointCommandWriteBO
     * @return generated commandId for status tracking
     */
    String write(Long tenantId, PointCommandWriteBO entityBO);

    /**
     * Write command from trusted internal callers.
     *
     * @param entityBO PointCommandWriteBO
     * @return generated commandId
     */
    default String write(PointCommandWriteBO entityBO) {
        return write(null, entityBO);
    }

}
