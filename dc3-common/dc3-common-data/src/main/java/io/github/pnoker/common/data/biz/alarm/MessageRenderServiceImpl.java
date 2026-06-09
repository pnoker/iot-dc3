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
import io.github.pnoker.common.entity.ext.MessageExt;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Structured message renderer.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class MessageRenderServiceImpl implements MessageRenderService {

    private final AlarmTemplateRenderer alarmTemplateRenderer;

    @Override
    public MessagePayload render(MessageBO message, NotifyChannelTypeEnum channelTypeFlag,
                                 Map<String, Object> variables) {
        MessageExt.Template template = selectTemplate(message, channelTypeFlag);
        if (Objects.isNull(template)) {
            return new MessagePayload(channelTypeFlag, null, Map.of(), List.of());
        }

        Map<String, Object> safeVariables = Objects.requireNonNullElse(variables, Map.of());
        List<String> missingVariables = missingVariables(message, safeVariables);
        Map<String, Object> rendered = alarmTemplateRenderer.renderMap(template.getTemplate(), safeVariables);
        return new MessagePayload(channelTypeFlag, template.getPayloadType(), rendered, missingVariables);
    }

    private MessageExt.Template selectTemplate(MessageBO message, NotifyChannelTypeEnum channelTypeFlag) {
        if (Objects.isNull(message) || Objects.isNull(channelTypeFlag) || Objects.isNull(message.getMessageExt())
                || Objects.isNull(message.getMessageExt().getContent())
                || Objects.isNull(message.getMessageExt().getContent().getTemplates())) {
            return null;
        }

        for (MessageExt.Template template : message.getMessageExt().getContent().getTemplates()) {
            if (matchesChannel(template, channelTypeFlag)) {
                return template;
            }
        }
        return null;
    }

    private boolean matchesChannel(MessageExt.Template template, NotifyChannelTypeEnum channelTypeFlag) {
        if (Objects.isNull(template) || StringUtils.isBlank(template.getChannelType())) {
            return false;
        }
        return StringUtils.equalsIgnoreCase(template.getChannelType(), channelTypeFlag.name())
                || StringUtils.equalsIgnoreCase(template.getChannelType(), channelTypeFlag.getCode());
    }

    private List<String> missingVariables(MessageBO message, Map<String, Object> variables) {
        if (Objects.isNull(message) || Objects.isNull(message.getMessageExt())
                || Objects.isNull(message.getMessageExt().getContent())
                || Objects.isNull(message.getMessageExt().getContent().getVariables())) {
            return List.of();
        }

        List<String> missing = new ArrayList<>();
        for (String variable : message.getMessageExt().getContent().getVariables()) {
            if (!variables.containsKey(variable)) {
                missing.add(variable);
            }
        }
        return missing;
    }

}
