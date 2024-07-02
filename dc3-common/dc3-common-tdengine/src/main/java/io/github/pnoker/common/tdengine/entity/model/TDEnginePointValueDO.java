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

package io.github.pnoker.common.tdengine.entity.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * TDEngine 位号数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
public class TDEnginePointValueDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String id;

    /**
     * 设备ID
     */
    private Long deviceId;

    /**
     * 位号ID
     */
    private Long pointId;

    /**
     * 原始值
     */
    private String rawValue;

    /**
     * 处理值
     */
    private String value;

    /**
     * 原始时间
     */
    private LocalDateTime originTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 操作时间
     */
    private LocalDateTime operateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS", timezone = "GMT+8")
    private Timestamp ts;
}