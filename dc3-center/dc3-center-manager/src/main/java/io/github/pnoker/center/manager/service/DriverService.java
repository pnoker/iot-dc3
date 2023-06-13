/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.service;

import io.github.pnoker.center.manager.entity.query.DriverPageQuery;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.model.DriverDO;

import java.util.List;
import java.util.Set;

/**
 * Driver Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverService extends Service<DriverDO, DriverPageQuery> {

    /**
     * 根据 驱动Id集 查询 驱动集
     *
     * @param ids Driver ID Array
     * @return Driver Array
     */
    List<DriverDO> selectByIds(Set<String> ids);

    /**
     * 根据 驱动ServiceName 查询 驱动
     *
     * @param serviceName    驱动服务名称
     * @param tenantId       租户ID
     * @param throwException Throw Exception
     * @return Driver
     */
    DriverDO selectByServiceName(String serviceName, String tenantId, boolean throwException);

    /**
     * 根据 模版Id 查询 驱动集
     *
     * @param profileId Profile ID
     * @return Driver Array
     */
    List<DriverDO> selectByProfileId(String profileId);

    /**
     * 根据 驱动Id 查询 驱动
     *
     * @param deviceId 设备ID
     * @return Driver
     */
    DriverDO selectByDeviceId(String deviceId);

    Long count();
}
