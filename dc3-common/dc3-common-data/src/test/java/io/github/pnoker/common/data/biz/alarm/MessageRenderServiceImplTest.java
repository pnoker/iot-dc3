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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRenderServiceImplTest {

    private final MessageRenderServiceImpl service = new MessageRenderServiceImpl(new AlarmTemplateRenderer());

    @Test
    void rendersStructuredTemplateForSelectedChannel() {
        MessageBO message = message();

        MessagePayload payload = service.render(message, NotifyChannelTypeEnum.FEISHU_BOT, Map.of(
                "severity", "P1",
                "deviceName", "Line A PLC",
                "value", 86.5,
                "unit", "C"));

        assertThat(payload.getPayloadType()).isEqualTo("CARD");
        assertThat(payload.getMissingVariables()).isEmpty();
        assertThat(payload.getPayload()).containsEntry("title", "P1 Line A PLC");
        assertThat(payload.getPayload()).containsEntry("summary", "Current value is 86.5 C");
    }

    @Test
    void keepsUnknownPlaceholdersAndReportsMissingVariables() {
        MessageBO message = message();

        MessagePayload payload = service.render(message, NotifyChannelTypeEnum.FEISHU_BOT, Map.of(
                "severity", "P1",
                "deviceName", "Line A PLC"));

        assertThat(payload.getMissingVariables()).containsExactly("value", "unit");
        assertThat(payload.getPayload()).containsEntry("summary", "Current value is ${value} ${unit}");
    }

    private MessageBO message() {
        MessageExt.Content content = new MessageExt.Content(
                List.of("severity", "deviceName", "value", "unit"),
                List.of(new MessageExt.Template(
                        "FEISHU_BOT",
                        "CARD",
                        Map.of(
                                "title", "${severity} ${deviceName}",
                                "summary", "Current value is ${value} ${unit}"))));
        MessageExt ext = new MessageExt(content);
        ext.setType("ALARM_MESSAGE_TEMPLATE");
        ext.setVersion(1);

        MessageBO message = new MessageBO();
        message.setMessageCode("ops-critical-card");
        message.setMessageExt(ext);
        return message;
    }

}
