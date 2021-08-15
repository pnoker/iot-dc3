/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.model;

import com.dc3.common.constant.Common;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author pnoker
 */
@Data
@Document
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointValue implements Serializable {
    private static final long serialVersionUID = 1L;

    @MongoId
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
    private String value;

    /**
     * 原始值
     */
    private String rawValue;

    /**
     * 计算值
     */
    private Object calculateValue;

    private List<PointValue> children;

    @Transient
    private Short rw;

    @Transient
    private String unit;

    @Transient
    private String type;

    @Transient
    private Integer timeOut = 15;

    @Transient
    private TimeUnit timeUnit = TimeUnit.MINUTES;

    private Boolean multi;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = Common.DATE_FORMAT, timezone = Common.TIMEZONE)
    private Date originTime;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = Common.DATE_FORMAT, timezone = Common.TIMEZONE)
    private Date createTime;

    public PointValue(Long pointId, String rawValue, String value) {
        this.pointId = pointId;
        this.rawValue = rawValue;
        this.value = value;
    }

    public PointValue(Long deviceId, Long pointId, String rawValue, String value) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.rawValue = rawValue;
        this.value = value;
        this.originTime = new Date();
    }

    public PointValue(Long deviceId, Long pointId, String rawValue, String value, int timeOut, TimeUnit timeUnit) {
        this.deviceId = deviceId;
        this.pointId = pointId;
        this.rawValue = rawValue;
        this.value = value;
        this.timeOut = timeOut;
        this.timeUnit = timeUnit;
        this.originTime = new Date();
    }

    public PointValue(Long deviceId, List<PointValue> children) {
        this.deviceId = deviceId;
        this.children = children;
        this.originTime = new Date();
    }

    public PointValue(Long deviceId, List<PointValue> children, int timeOut, TimeUnit timeUnit) {
        this.deviceId = deviceId;
        this.children = children;
        this.timeOut = timeOut;
        this.timeUnit = timeUnit;
        this.originTime = new Date();
    }
}
