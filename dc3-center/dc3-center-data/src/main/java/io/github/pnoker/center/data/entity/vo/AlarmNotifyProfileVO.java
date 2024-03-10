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

package io.github.pnoker.center.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.AlarmNotifyExt;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <p>
 * 报警通知模板表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmNotifyProfile", description = "报警通知模板")
public class AlarmNotifyProfileVO extends BaseVO {

    /**
     * 报警通知模板名称
     */
    @Schema(description = "报警通知模板名称")
    private String alarmNotifyName;

    /**
     * 报警通知模板编号
     */
    @Schema(description = "报警通知模板编号")
    private String alarmNotifyCode;

    /**
     * 自动确认标识
     */
    @Schema(description = "自动确认标识")
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 报警通知间隔，毫秒
     */
    @Schema(description = "报警通知间隔，毫秒")
    private Long alarmNotifyInterval;

    /**
     * 报警通知模版配置
     */
    @Schema(description = "报警通知模版配置")
    private AlarmNotifyExt alarmNotifyExt;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}
