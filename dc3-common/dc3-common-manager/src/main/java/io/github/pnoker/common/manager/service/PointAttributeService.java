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
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;

import java.util.List;

/**
 * 位号属性Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointAttributeService extends BaseService<PointAttributeBO, PointAttributeQuery> {
    /**
     * 根据 位号配置属性名称 和 驱动ID 查询
     *
     * @param name     属性名称
     * @param driverId 驱动ID
     * @return PointAttribute
     */
    PointAttributeBO selectByNameAndDriverId(String name, Long driverId);

    /**
     * 根据 驱动ID 查询
     *
     * @param driverId 驱动ID
     * @return 位号属性Array
     */
    List<PointAttributeBO> selectByDriverId(Long driverId);
}
