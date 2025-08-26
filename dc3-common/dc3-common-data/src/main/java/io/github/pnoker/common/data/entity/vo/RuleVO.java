/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.data.entity.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.pnoker.common.entity.base.BaseVO;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * <p>
 * 报警规则表
 * </p>
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class RuleVO extends BaseVO {

    /**
     * 实体类型标识
     */
    private AlarmTypeFlagEnum entityTypeFlag;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则编号
     */
    private String ruleCode;

    /**
     * 实体ID
     */
    private Long entityId;

    /**
     * 报警通知模板ID
     */
    private Long notifyId;

    /**
     * 报警信息模板ID
     */
    private Long messageId;

    /**
     * 报警规则
     */
    private RuleExt ruleExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
