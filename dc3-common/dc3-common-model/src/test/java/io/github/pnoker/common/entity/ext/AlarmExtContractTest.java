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

package io.github.pnoker.common.entity.ext;

import io.github.pnoker.common.enums.AlarmTargetTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AlarmExtContractTest {

    @Test
    void ruleContentRoundTripsAsStructuredCondition() {
        RuleExt.Content content = new RuleExt.Content(
                new RuleExt.Condition("numValue", ">", null, BigDecimal.valueOf(80), null, null, "C"),
                new RuleExt.Window("ALL", "PT3M", 3),
                new RuleExt.Recovery(true, "<=", BigDecimal.valueOf(75), "PT2M"),
                "P1",
                "ALARM",
                List.of("temperature", "line-a"));

        RuleExt.Content parsed = JsonUtil.parseObject(JsonUtil.toJsonString(content), RuleExt.Content.class);

        assertThat(parsed.getCondition().getField()).isEqualTo("numValue");
        assertThat(parsed.getCondition().getThreshold()).isEqualByComparingTo("80");
        assertThat(parsed.getWindow().getDuration()).isEqualTo("PT3M");
        assertThat(parsed.getRecovery().getOperator()).isEqualTo("<=");
        assertThat(parsed.getLabels()).containsExactly("temperature", "line-a");
    }

    @Test
    void notifyContentRoundTripsAsStructuredPolicy() {
        NotifyExt.Content content = new NotifyExt.Content(
                new NotifyExt.Dedup(true, "${tenantId}:${ruleCode}:${entityId}"),
                new NotifyExt.RateLimit(300000L, 1),
                new NotifyExt.Silence(false, List.of()),
                new NotifyExt.Repeat(true, 1800000L, 0),
                new NotifyExt.Recovery(true, true, false),
                List.of(new NotifyExt.Escalation(900000L, "ops-critical-card")));

        NotifyExt.Content parsed = JsonUtil.parseObject(JsonUtil.toJsonString(content), NotifyExt.Content.class);

        assertThat(parsed.getDedup().getKey()).isEqualTo("${tenantId}:${ruleCode}:${entityId}");
        assertThat(parsed.getRateLimit().getIntervalMs()).isEqualTo(300000L);
        assertThat(parsed.getRepeat().getEnabled()).isTrue();
        assertThat(parsed.getEscalation()).hasSize(1);
    }

    @Test
    void messageContentRoundTripsAsChannelTemplates() {
        MessageExt.Content content = new MessageExt.Content(
                List.of("severity", "device", "value"),
                List.of(new MessageExt.Template(
                        "FEISHU_BOT",
                        "CARD",
                        Map.of(
                                "title", "${severity} ${device}",
                                "summary", "Current value is ${value}"))));

        MessageExt.Content parsed = JsonUtil.parseObject(JsonUtil.toJsonString(content), MessageExt.Content.class);

        assertThat(parsed.getVariables()).containsExactly("severity", "device", "value");
        assertThat(parsed.getTemplates()).hasSize(1);
        assertThat(parsed.getTemplates().get(0).getChannelType()).isEqualTo("FEISHU_BOT");
        assertThat(parsed.getTemplates().get(0).getTemplate()).containsEntry("title", "${severity} ${device}");
    }

    @Test
    void notifyChannelContentRoundTripsWithoutSecrets() {
        NotifyChannelExt.Content content = new NotifyChannelExt.Content(
                true,
                "interactive-card-v1",
                false,
                true,
                Map.of("locale", "zh-CN"));

        NotifyChannelExt.Content parsed = JsonUtil.parseObject(
                JsonUtil.toJsonString(content), NotifyChannelExt.Content.class);

        assertThat(parsed.getSignEnabled()).isTrue();
        assertThat(parsed.getOptions()).containsEntry("locale", "zh-CN");
    }

    @Test
    void runtimeStateContentRoundTripsAsStructuredSnapshot() {
        RuleStateExt.Content content = new RuleStateExt.Content(
                "temperature-high",
                "P1",
                "ALARM",
                List.of("temperature", "line-a"),
                Map.of("pointId", 1001L, "numValue", 86.5),
                "FIRING",
                Map.of("dedupKey", "tenant:rule:point"));

        RuleStateExt.Content parsed = JsonUtil.parseObject(
                JsonUtil.toJsonString(content), RuleStateExt.Content.class);

        assertThat(parsed.getRuleCode()).isEqualTo("temperature-high");
        assertThat(parsed.getLabels()).containsExactly("temperature", "line-a");
        assertThat(parsed.getLastFact()).containsEntry("pointId", 1001);
        assertThat(parsed.getMatchType()).isEqualTo("FIRING");
        assertThat(parsed.getMetadata()).containsEntry("dedupKey", "tenant:rule:point");
    }

    @Test
    void ruleAlarmEventContentRoundTripsAsStructuredSnapshot() {
        RuleAlarmEventExt.Content content = new RuleAlarmEventExt.Content(
                1L,
                "temperature-high",
                "Temperature High",
                AlarmTargetTypeEnum.POINT,
                1001L,
                "P1",
                "temperature_high",
                "FIRING",
                Map.of("deviceId", 10L, "numValue", 86.5));

        RuleAlarmEventExt.Content parsed = JsonUtil.parseObject(
                JsonUtil.toJsonString(content), RuleAlarmEventExt.Content.class);

        assertThat(parsed.getRuleCode()).isEqualTo("temperature-high");
        assertThat(parsed.getTargetType()).isEqualTo(AlarmTargetTypeEnum.POINT);
        assertThat(parsed.getValues()).containsEntry("deviceId", 10);
    }

    @Test
    void notifyHistoryExtRoundTripsAsRequestAndResponse() {
        NotifyHistoryRequestExt.Content request = new NotifyHistoryRequestExt.Content(
                "P1 Temperature Alarm",
                "Line A temperature is 86.5 C",
                "CARD",
                Map.of("deviceName", "Line A PLC", "value", 86.5),
                Map.of("msg_type", "interactive"));
        NotifyHistoryResponseExt.Content response = new NotifyHistoryResponseExt.Content(
                "om_123",
                200,
                "ok",
                Map.of("code", 0));

        NotifyHistoryRequestExt.Content parsedRequest = JsonUtil.parseObject(
                JsonUtil.toJsonString(request), NotifyHistoryRequestExt.Content.class);
        NotifyHistoryResponseExt.Content parsedResponse = JsonUtil.parseObject(
                JsonUtil.toJsonString(response), NotifyHistoryResponseExt.Content.class);

        assertThat(parsedRequest.getTitle()).isEqualTo("P1 Temperature Alarm");
        assertThat(parsedRequest.getPayload()).containsEntry("msg_type", "interactive");
        assertThat(parsedResponse.getProviderMessageId()).isEqualTo("om_123");
        assertThat(parsedResponse.getPayload()).containsEntry("code", 0);
    }

}
