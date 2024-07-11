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

package io.github.pnoker.common.data.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;

import java.util.List;

/**
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointValueService {

    /**
     * 新增 PointValue
     *
     * @param pointValueBO PointValue
     */
    void save(PointValueBO pointValueBO);

    /**
     * 批量新增 PointValue
     *
     * @param pointValueBOList Array
     */
    void save(List<PointValueBO> pointValueBOList);

    /**
     * 获取历史 PointValue
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @param count    数量
     * @return History Value Array
     */
    List<String> history(Long deviceId, Long pointId, int count);

    /**
     * 获取带分页, 排序 最新 PointValue
     *
     * @param pointValueQuery Entry of Query
     * @return Entity of BO Page
     */
    Page<PointValueBO> latest(PointValueQuery pointValueQuery);

    /**
     * 获取带分页, 排序 历史 PointValue
     *
     * @param pointValueQuery Entry of Query
     * @return Entity of BO Page
     */
    Page<PointValueBO> page(PointValueQuery pointValueQuery);

}
