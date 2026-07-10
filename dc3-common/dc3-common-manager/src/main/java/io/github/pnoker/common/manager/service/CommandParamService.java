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
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.query.CommandParamQuery;

import java.util.List;
import java.util.Set;

/**
 * Business service for command param operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface CommandParamService extends BaseService<CommandParamBO, CommandParamQuery> {

    /**
     * Query command params belonging to a command.
     *
     * @param commandId Command ID
     * @return CommandParamBO list
     */
    List<CommandParamBO> listByCommandId(Long commandId);

    /**
     * Query command params by a set of param IDs.
     *
     * @param ids Param ID set
     * @return CommandParamBO list
     */
    List<CommandParamBO> listByIds(Set<Long> ids);

}
