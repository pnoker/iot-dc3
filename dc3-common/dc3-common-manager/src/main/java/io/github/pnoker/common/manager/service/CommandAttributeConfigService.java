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
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.query.CommandAttributeConfigQuery;

import java.util.List;

/**
 * Business service for command attribute configuration operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface CommandAttributeConfigService extends BaseService<CommandAttributeConfigBO, CommandAttributeConfigQuery> {

    /**
     * Device ID
     *
     * @param deviceId Device ID
     * @return CommandConfig
     */
    List<CommandAttributeConfigBO> listByDeviceId(Long deviceId);

    /**
     * ID
     *
     * @param attributeId ID
     * @return CommandConfig
     */
    List<CommandAttributeConfigBO> listByAttributeId(Long attributeId);

    /**
     * Device ID Command ID
     *
     * @param deviceId Device ID
     * @param commandId  Command ID
     * @return CommandConfig
     */
    List<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long deviceId, Long commandId);

    /**
     * ID Device ID Command ID
     *
     * @param attributeId ID
     * @param deviceId    Device ID
     * @param commandId     Command ID
     * @return CommandConfig
     */
    CommandAttributeConfigBO getByAttributeIdAndDeviceIdAndCommandId(Long attributeId, Long deviceId, Long commandId);

    /**
     * @param entityBO {@link CommandAttributeConfigBO}
     * @return {@link DeviceBO}
     */
    CommandAttributeConfigBO innerSave(CommandAttributeConfigBO entityBO);

}
