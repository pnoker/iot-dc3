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

package io.github.pnoker.common.auth.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ExpireFlagEnum;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DriverToken VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class DriverTokenVO extends BaseVO {

    /**
     * 驱动编号
     */
    private String driverCode;

    /**
     * 驱动AppID
     */
    private String driverAppId;

    /**
     * 驱动AppKey
     */
    private String driverAppKey;

    /**
     * 失效标识
     */
    private ExpireFlagEnum expireFlag;

    /**
     * 失效时间
     */
    private LocalDateTime expireTime;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
