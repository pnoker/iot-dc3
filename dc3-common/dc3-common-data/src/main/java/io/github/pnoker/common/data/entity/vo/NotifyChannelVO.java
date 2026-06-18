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
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Notify Channel view object")
public class NotifyChannelVO extends BaseVO {

    @Schema(description = "Notification channel name. Unique name within a tenant.", example = "SMS Alert Channel", requiredMode = Schema.RequiredMode.REQUIRED)

    private String channelName;

    @Schema(description = "Notification channel code. Stable business identifier; must not change once deployed.", example = "SMS_CHANNEL")

    private String channelCode;

    @Schema(description = "Notification channel type enum")

    private NotifyChannelTypeEnum channelTypeFlag;

    @Schema(description = "Credential reference pointing to the authentication configuration for this channel (e.g. SMS gateway API key).", example = "cred_sms_001")
    @ToString.Exclude
    private String credentialRef;

    @Schema(description = "Notification channel extension information, serialized as JSON for custom delivery configuration.")

    private NotifyChannelExt channelExt;

    @Schema(description = "Enable flag: ENABLE (0) or DISABLE (1).", example = "ENABLE")

    private EnableFlagEnum enableFlag;

}
