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
import io.github.pnoker.common.entity.ext.NotifyChannelExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for notification channel API responses.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class NotifyChannelVO extends BaseVO {

    private String channelName;

    private String channelCode;

    private NotifyChannelTypeFlagEnum channelTypeFlag;

    private String credentialRef;

    private NotifyChannelExt channelExt;

    private EnableFlagEnum enableFlag;

}
