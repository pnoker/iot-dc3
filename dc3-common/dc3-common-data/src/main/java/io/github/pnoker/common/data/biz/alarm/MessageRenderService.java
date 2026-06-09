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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;

import java.util.Map;

/**
 * Renders structured message templates into channel payloads.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface MessageRenderService {

    /**
     * Render a message template for a channel type.
     *
     * @param message         message template
     * @param channelTypeFlag channel type
     * @param variables       rendering variables
     * @return rendered payload
     */
    MessagePayload render(MessageBO message, NotifyChannelTypeEnum channelTypeFlag, Map<String, Object> variables);

}
