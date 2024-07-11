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

package io.github.pnoker.common.data.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * <p>
 * 报警规则表
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class AlarmRuleQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 租户ID
     */
    private Long tenantId;

    // 查询字段

    /**
     * 报警规则名称
     */
    private String alarmRuleName;

    /**
     * 报警规则编号
     */
    private String alarmRuleCode;

    /**
     * 位号ID
     */
    private Long pointId;

    /**
     * 报警类型标识
     */
    private AlarmTypeFlagEnum alarmTypeFlag;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
