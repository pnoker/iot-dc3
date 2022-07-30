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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.dto.PointValueDto;

import java.util.List;

/**
 * @author pnoker
 */
public interface PointValueService {

    /**
     * 新增 PointValue
     *
     * @param pointValue PointValue
     */
    void savePointValue(PointValue pointValue);

    /**
     * 批量新增 PointValue
     *
     * @param pointValues PointValue Array
     */
    void savePointValues(List<PointValue> pointValues);

    /**
     * 获取实时数据
     *
     * @param deviceId Device Id
     * @return PointValue Array
     */
    List<PointValue> realtime(String deviceId);

    /**
     * 获取实时数据
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    PointValue realtime(String deviceId, String pointId);

    /**
     * 获取一个设备最新的位号数据集合
     *
     * @param deviceId Device Id
     * @return PointValue Array
     */
    List<PointValue> latest(String deviceId);

    /**
     * 获取最新的一个位号数据
     *
     * @param deviceId Device Id
     * @param pointId  Point Id
     * @return PointValue
     */
    PointValue latest(String deviceId, String pointId);

    /**
     * 获取带分页、排序
     *
     * @param pointValueDto PointValueDto
     * @return Page<PointValue>
     */
    Page<PointValue> list(PointValueDto pointValueDto);

}
