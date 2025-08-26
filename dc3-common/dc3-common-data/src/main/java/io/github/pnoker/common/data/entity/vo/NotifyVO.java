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
import io.github.pnoker.common.entity.ext.NotifyExt;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

/**
 * <p>
 * 报警通知模板表
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
public class NotifyVO extends BaseVO {

    /**
     * 报警通知模板名称
     */
    private String notifyName;

    /**
     * 报警通知模板编号
     */
    private String notifyCode;

    /**
     * 自动确认标识
     */
    private AutoConfirmFlagEnum autoConfirmFlag;

    /**
     * 报警通知间隔, 毫秒
     */
    private Long notifyInterval;

    /**
     * 报警通知模版配置
     */
    private NotifyExt notifyExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;
}
