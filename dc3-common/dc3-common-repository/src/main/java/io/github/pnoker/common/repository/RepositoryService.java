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

package io.github.pnoker.common.repository;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;

import java.io.IOException;
import java.util.List;

/**
 * 数据存储策略服务接口
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface RepositoryService {

    /**
     * 获取存储策略服务名称
     *
     * @return Repository Name
     */
    String getRepositoryName();

    /**
     * 保存 PointValue
     *
     * @param entityBO PointValue
     * @throws IOException IOException
     */
    void savePointValue(PointValueBO entityBO) throws IOException;

    /**
     * 保存 PointValue 集合
     *
     * @param deviceId     设备ID
     * @param entityBOList PointValue Array
     * @throws IOException IOException
     */
    void savePointValue(Long deviceId, List<PointValueBO> entityBOList) throws IOException;

    /**
     * 获取历史 PointValue
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @param count    数量
     * @return History Value Array
     */
    List<String> selectHistoryPointValue(Long deviceId, Long pointId, int count);

    /**
     * 查询最新 PointValue
     *
     * @param deviceId 设备ID
     * @param pointId  位号ID
     * @return PointValueBO Array
     */
    PointValueBO selectLatestPointValue(Long deviceId, Long pointId);

    /**
     * 查询最新 PointValue
     *
     * @param deviceId 设备ID
     * @param pointIds 位号ID集
     * @return PointValueBO Array
     */
    List<PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds);

    /**
     * 分页查询 PointValue
     *
     * @param entityQuery Entry of Query
     * @return Entity of BO Page
     */
    Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery);
}
