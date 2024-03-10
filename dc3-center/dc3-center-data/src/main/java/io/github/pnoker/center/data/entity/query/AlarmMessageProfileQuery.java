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

package io.github.pnoker.center.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.io.Serializable;

/**
 * <p>
 * 报警信息模板表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(title = "AlarmMessageProfile", description = "报警信息模板")
public class AlarmMessageProfileQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "分页")
    private Pages page;

    /**
     * 租户ID
     */
    @Schema(description = "使能标识", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Long tenantId;

    // 查询字段

    /**
     * 报警信息模板标题
     */
    @Schema(description = "报警标题")
    private String alarmMessageTitle;

    /**
     * 报警信息模板编号
     */
    @Schema(description = "报警信息模板编号")
    private String alarmMessageCode;

    /**
     * 报警信息模板等级
     */
    @Schema(description = "报警信息模板等级")
    private AlarmMessageLevelFlagEnum alarmMessageLevel;

    /**
     * 使能标识
     */
    @Schema(description = "使能标识")
    private EnableFlagEnum enableFlag;
}
