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

package io.github.pnoker.center.data.service;

import io.github.pnoker.common.bean.point.PointValue;

import java.util.List;

/**
 * Point Value 存储策略服务接口
 *
 * @author pnoker
 */
public interface RepositoryService {

    /**
     * 保存 PointValue
     *
     * @param pointValue PointValue
     */
    void savePointValue(PointValue pointValue);

    /**
     * 保存 PointValue 集合
     *
     * @param deviceId    Device Id
     * @param pointValues PointValue Array
     */
    void savePointValues(String deviceId, List<PointValue> pointValues);
}
