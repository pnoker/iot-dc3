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
import io.github.pnoker.center.manager.entity.query.PointBOPageQuery;
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
public interface PointService extends Service<PointBO, PointBOPageQuery> {

    /**
     * 根据 位号Name 、 模板Id 查询位号
     *
     * @param name      Point Name
     * @param profileId Profile ID
     * @return Point
     */
    PointBO selectByNameAndProfileId(String name, Long profileId);

    /**
     * 根据 设备Id 查询位号
     *
     * @param deviceId 设备ID
     * @return Point Array
     */
    List<PointBO> selectByDeviceId(Long deviceId);

    /**
     * 根据 模板Id 查询位号
     *
     * @param profileId Profile ID
     * @return Point Array
     */
    List<PointBO> selectByProfileId(Long profileId);

    /**
     * 根据 模板Id 集查询位号
     *
     * @param profileIds     Profile ID Set
     * @param throwException Throw Exception
     * @return Point Array
     */
    List<PointBO> selectByProfileIds(Set<Long> profileIds, boolean throwException);

    /**
     * 根据 设备Id集 查询设备
     *
     * @param ids Point ID Set
     * @return Point Array
     */
    List<PointBO> selectByIds(Set<Long> ids);

    /**
     * 查询 位号单位
     *
     * @param pointIds Point ID Set
     * @return Map Long:Unit String
     */
    Map<Long, String> unit(Set<Long> pointIds);

    Long count();
}
