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

package io.github.pnoker.center.manager.service;

import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.common.base.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Point Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointService extends Service<PointBO, PointQuery> {

    /**
     * 根据 设备ID 查询位号
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    List<PointBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 模板ID 查询位号
     *
     * @param profileId 位号ID
     * @return Point Array
     */
    List<PointBO> selectByProfileId(Long profileId);

    /**
     * 根据 模板ID 集查询位号
     *
     * @param profileIds     Profile ID集
     * @param throwException Throw Exception
     * @return Point Array
     */
    List<PointBO> selectByProfileIds(Set<Long> profileIds);

    /**
     * 根据 设备ID集 查询设备
     *
     * @param ids 位号ID集
     * @return Point Array
     */
    List<PointBO> selectByIds(Set<Long> ids);

    /**
     * 查询 位号单位
     *
     * @param pointIds 位号ID集
     * @return Map Long:Unit String
     */
    Map<Long, String> unit(Set<Long> pointIds);

}
