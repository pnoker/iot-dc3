/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.common.bean.driver.PointValue;
import com.dc3.common.bean.driver.PointValueDto;

/**
 * @author pnoker
 */
public interface PointValueService {
    /**
     * 新增 PointValue
     *
     * @param pointValue
     */
    void add(PointValue pointValue);

    /**
     * 获取带分页、排序
     *
     * @param pointValueDto
     * @return
     */
    Page<PointValue> list(PointValueDto pointValueDto);

    /**
     * 获取最新的一个位号数据
     *
     * @param pointValueDto
     * @return
     */
    PointValue latest(PointValueDto pointValueDto);
}
