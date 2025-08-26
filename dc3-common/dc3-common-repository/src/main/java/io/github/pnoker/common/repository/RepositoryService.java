/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
     * @param entityBOList PointValue Array
     * @throws IOException IOException
     */
    void savePointValues(List<PointValueBO> entityBOList) throws IOException;

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
    List<PointValueBO> selectLatestPointValues(Long deviceId, List<Long> pointIds);

    /**
     * 分页查询 PointValue
     *
     * @param entityQuery Entry of Query
     * @return Entity of BO Page
     */
    Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery);
}
