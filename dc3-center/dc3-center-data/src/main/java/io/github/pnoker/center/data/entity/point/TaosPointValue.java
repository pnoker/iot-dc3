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

package io.github.pnoker.center.data.entity.point;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.sql.Timestamp;

/**
 * TDengine 位号数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaosPointValue {

    @TableId
    private String id;

    /**
     * 设备ID，同MySQl中等 设备ID 一致
     */
    private Long deviceId;

    /**
     * 位号ID，同MySQl中等 位号ID 一致
     */
    private Long pointId;

    /**
     * 处理值，进行过缩放、格式化等操作
     */
    private String pointValue;

    /**
     * 原始值
     */
    private String rawValue;

    private Timestamp createTime;

    private Timestamp originTime;

    public TaosPointValue(PointValue pointValue) {
        BeanUtil.copyProperties(pointValue, this);
        this.setPointValue(pointValue.getValue());
        this.setCreateTime(new Timestamp(pointValue.getCreateTime().getTime()));
        this.setOriginTime(new Timestamp(pointValue.getOriginTime().getTime()));
    }
}
