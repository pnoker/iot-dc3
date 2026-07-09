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
import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;

/**
 * Business service for custom command call operations.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public interface CommandHistoryService {

    /**
     * Submit a custom command call: validate scope, resolve the driver, persist a
     * pending history record, and dispatch it to the driver, returning the record id.
     *
     * @param tenantId tenant scope
     * @param entityBO {@link CommandCallBO} command call payload
     * @return the record id assigned to the call
     */
    String call(Long tenantId, CommandCallBO entityBO);

    /**
     * Get a single command history record by record id, scoped to a tenant.
     *
     * @param tenantId tenant scope; null queries across tenants
     * @param recordId the record id
     * @return the command history, or {@code null} if not found
     */
    CommandHistoryVO getByRecordId(Long tenantId, String recordId);

    /**
     * Get a single command history record by record id across tenants.
     *
     * @param recordId the record id
     * @return the command history, or {@code null} if not found
     */
    default CommandHistoryVO getByRecordId(String recordId) {
        return getByRecordId(null, recordId);
    }

    /**
     * Page command history records by query, scoped to a tenant.
     *
     * @param tenantId tenant scope
     * @param queryVO  filter and pagination parameters
     * @return the matching command history page
     */
    Page<CommandHistoryVO> list(Long tenantId, CommandHistoryQueryVO queryVO);

}
