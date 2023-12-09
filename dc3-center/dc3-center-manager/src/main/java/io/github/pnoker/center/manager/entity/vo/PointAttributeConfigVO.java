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
import io.github.pnoker.common.valid.Insert;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * PointAttributeConfig BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Data
@SuperBuilder
@RequiredArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "PointAttributeConfig", description = "位号属性配置")
public class PointAttributeConfigVO extends BaseVO {

    /**
     * 位号属性ID
     */@Schema(description = "位号属性ID")
    @NotBlank(message = "位号属性ID不能为空",
            groups = {Insert.class, Update.class})
    private Long pointAttributeId;

    /**
     * 位号属性配置值
     */@Schema(description = "位号属性配置值")
    @NotNull(message = "位号属性配置值不能为空")
    private String configValue;

    /**
     * 设备ID
     */@Schema(description = "设备ID")
    @NotBlank(message = "设备ID不能为空",
            groups = {Insert.class, Update.class})
    private Long deviceId;

    /**
     * 位号ID
     */@Schema(description = "位号ID")
    @NotBlank(message = "位号ID不能为空",
            groups = {Insert.class, Update.class})
    private Long pointId;

    /**
     * 使能标识
     */@Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */@Schema(description = "租户ID")
    private Long tenantId;
}
