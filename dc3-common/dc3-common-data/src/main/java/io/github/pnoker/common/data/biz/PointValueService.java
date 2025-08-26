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

package io.github.pnoker.common.data.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;

import java.util.List;

/**
 * @author pnoker
 * @version 2025.6.0
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
