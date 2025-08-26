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

import io.github.pnoker.common.data.entity.bo.DeviceRunBO;
import io.github.pnoker.common.data.entity.query.DeviceQuery;

import java.util.Map;

/**
 * 设备 Interface
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DeviceStatusService {

    /**
     * 分页查询设备状态, 同设备分页查询配套使用
     *
     * @param deviceQuery 设备和分页参数
     * @return Map String:String
     */
    Map<Long, String> selectByPage(DeviceQuery deviceQuery);

    /**
     * 根据 模板ID 查询设备状态
     *
     * @param profileId 位号ID
     * @return Map String:String
     */
    Map<Long, String> selectByProfileId(Long profileId);

    DeviceRunBO selectOnlineByDeviceId(Long deviceId);

    DeviceRunBO selectOfflineByDeviceId(Long deviceId);
}
