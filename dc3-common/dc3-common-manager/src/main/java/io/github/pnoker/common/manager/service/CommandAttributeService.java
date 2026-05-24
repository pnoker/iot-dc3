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

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.query.CommandAttributeQuery;

import java.util.Collection;
import java.util.List;

/**
 * Business service for command attribute operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface CommandAttributeService extends BaseService<CommandAttributeBO, CommandAttributeQuery> {

    /**
     * Driver ID
     *
     * @param driverId Driver ID
     * @return Array
     */
    List<CommandAttributeBO> listByDriverId(Long driverId);

    /**
     * Code Driver ID
     *
     * @param name     Code
     * @param driverId Driver ID
     * @return CommandAttribute
     */
    CommandAttributeBO getByNameAndDriverId(String name, Long driverId);

    /**
     * Bulk insert. Use during driver registration so attribute changes don't degenerate
     * into N round-trips.
     */
    void saveBatch(List<CommandAttributeBO> entityBOList);

    /**
     * Bulk update by id.
     */
    void updateBatch(List<CommandAttributeBO> entityBOList);

    /**
     * Bulk delete by id.
     */
    void removeByIds(Collection<Long> ids);

}
