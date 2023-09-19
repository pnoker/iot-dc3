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

package io.github.pnoker.center.data.mapper;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.pnoker.common.entity.point.TaosPointValue;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
@DS("taos")
public interface TaosPointValueMapper extends BaseMapper<TaosPointValue> {

    @Update("CREATE STABLE IF NOT EXISTS point_value (create_time TIMESTAMP, point_value NCHAR(32), raw_value NCHAR(32),  origin_time TIMESTAMP) TAGS (device_id NCHAR(32), point_id NCHAR(32))")
    int createSuperTable();

    @Update("CREATE TABLE IF NOT EXISTS point_value_${deviceId} using point_value TAGS (#{deviceId},#{pointId})")
    int createDeviceTable(@Param("deviceId") String deviceId, @Param("pointId") String pointId);

    @Insert("INSERT INTO point_value_${deviceId} (create_time,point_value,raw_value,origin_time) VALUES (#{createTime},#{pointValue},#{rawValue},#{originTime})")
    int insertOne(TaosPointValue taosPointValue);

    @Insert("<script>INSERT INTO point_value_${deviceId} (create_time,point_value,raw_value,origin_time) VALUES <foreach collection='list' item='item' index='index' separator=','>(#{item.createTime},#{item.pointValue},#{item.rawValue},#{item.originTime})</foreach></script>")
    int batchInsert(List<TaosPointValue> collect);
}
