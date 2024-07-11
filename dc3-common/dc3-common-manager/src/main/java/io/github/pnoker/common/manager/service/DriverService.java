/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;

import java.util.List;
import java.util.Set;

/**
 * Driver Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverService extends BaseService<DriverBO, DriverQuery> {

    /**
     * 根据 驱动ServiceName 查询 驱动
     *
     * @param serviceName 驱动服务名称
     * @param tenantId    租户ID
     * @return Driver
     */
    DriverBO selectByServiceName(String serviceName, Long tenantId);

    /**
     * 根据 模版ID 查询 驱动集
     *
     * @param profileId 模版ID
     * @return Driver 集合
     */
    List<DriverBO> selectByProfileId(Long profileId);

    /**
     * 根据 位号ID 查询 驱动集
     *
     * @param pointId 位号ID
     * @return Driver 集合
     */
    List<DriverBO> selectByPointId(Long pointId);

    /**
     * 根据 驱动ID 查询 驱动
     *
     * @param deviceId 设备ID
     * @return Driver
     */
    DriverBO selectByDeviceId(Long deviceId);

    /**
     * 根据 驱动ID集 查询 驱动集
     *
     * @param ids 驱动ID 集合
     * @return Driver 集合
     */
    List<DriverBO> selectByIds(Set<Long> ids);
}
