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

package io.github.pnoker.center.manager.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

/**
 * DriverAttributeConfig VO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "DriverAttributeConfig", description = "驱动属性配置")
public class DriverAttributeConfigVO extends BaseVO {

    /**
     * 驱动属性ID
     */
    @Schema(description = "驱动属性ID")
    @NotNull(message = "驱动属性ID不能为空",
            groups = {Add.class, Update.class})
    private Long driverAttributeId;

    /**
     * 驱动属性配置值
     */
    @Schema(description = "驱动属性配置值")
    @NotNull(message = "驱动属性配置值不能为空")
    private String configValue;

    /**
     * 设备ID
     */
    @Schema(description = "设备ID")
    @NotNull(message = "设备ID不能为空",
            groups = {Add.class, Update.class})
    private Long deviceId;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;

    /**
     * 签名
     */
    @Schema(description = "签名")
    private String signature;

    /**
     * 版本
     */
    @Schema(description = "版本")
    private Integer version;
}
