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

package io.github.pnoker.common.tdengine.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.tdengine.entity.model.TDEnginePointValueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
@DS("taos")
public interface TDEngineRepositoryMapper extends BaseMapper<TDEnginePointValueDO> {
    void savePointValue(@Param("tableName") String tableName, @Param("pointValueDO") TDEnginePointValueDO pointValueDO);

    void saveBatchPointValue(@Param("tableName") String tableName, @Param("tdEnginePointValueDOList") List<TDEnginePointValueDO> tdEnginePointValueDOList);

    List<TDEnginePointValueDO> selectHistoryPointValue(@Param("deviceId") Long deviceId, @Param("pointId") Long pointId, @Param("count") int count);

    List<TDEnginePointValueDO> selectLatestPointValue(@Param("deviceId") Long deviceId, @Param("pointIds") List<Long> pointIds);

    long count(@Param("entityQuery") PointValueQuery entityQuery);

    List<TDEnginePointValueDO> selectPagePointValue(@Param("entityQuery") PointValueQuery entityQuery, @Param("pages") Pages pages);
}
