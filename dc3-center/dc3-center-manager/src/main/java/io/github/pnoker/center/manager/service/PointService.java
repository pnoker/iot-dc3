/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.dto.PointDto;
import io.github.pnoker.common.model.Point;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Point Interface
 *
 * @author pnoker
 */
public interface PointService extends Service<Point, PointDto> {

    /**
     * 根据 位号Name & 模板Id 查询位号
     *
     * @param name      Point Name
     * @param profileId Profile Id
     * @return Point
     */
    Point selectByNameAndProfileId(String name, String profileId);

    /**
     * 根据 设备Id 查询位号
     *
     * @param deviceId Device Id
     * @return Point Array
     */
    List<Point> selectByDeviceId(String deviceId);

    /**
     * 根据 模板Id 查询位号
     *
     * @param profileId Profile Id
     * @return Point Array
     */
    List<Point> selectByProfileId(String profileId);

    /**
     * 根据 模板Id 集查询位号
     *
     * @param profileIds Profile Id Set
     * @return Point Array
     */
    List<Point> selectByProfileIds(Set<String> profileIds);

    /**
     * 根据 设备Id集 查询设备
     *
     * @param ids Point Id Set
     * @return Point Array
     */
    List<Point> selectByIds(Set<String> ids);

    /**
     * 查询 位号单位
     *
     * @param pointIds Point Id Set
     * @return Map<Long, String>
     */
    Map<String, String> unit(Set<String> pointIds);
}
