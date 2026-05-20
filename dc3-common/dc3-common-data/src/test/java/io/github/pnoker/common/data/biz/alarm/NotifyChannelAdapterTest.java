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

import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NotifyChannelAdapterTest {

    @Test
    void webhookAdapterFailsWhenCredentialIsMissing() {
        WebhookNotifyChannelAdapter adapter = new WebhookNotifyChannelAdapter(new OkHttpClient.Builder().build(),
                credentialRef -> Optional.empty());
        NotifyChannelBO channel = new NotifyChannelBO();
        channel.setCredentialRef("secret:webhook:missing");

        NotifySendResult result = adapter.send(channel, new MessagePayload(null, "JSON", Map.of("text", "hello"),
                java.util.List.of()));

        assertThat(result.getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.FAILED);
        assertThat(result.getErrorMessage()).contains("credential");
    }

    @Test
    void feishuAdapterFailsWhenSigningSecretIsMissing() {
        FeishuBotNotifyChannelAdapter adapter = new FeishuBotNotifyChannelAdapter(new OkHttpClient.Builder().build(),
                credentialRef -> Optional.of(new NotifyCredential("https://example.invalid/webhook", "", Map.of())));
        NotifyChannelBO channel = new NotifyChannelBO();
        channel.setCredentialRef("secret:feishu:missing-secret");
        io.github.pnoker.common.entity.ext.NotifyChannelExt ext = new io.github.pnoker.common.entity.ext.NotifyChannelExt();
        ext.setContent(new io.github.pnoker.common.entity.ext.NotifyChannelExt.Content(true, "v1", false, true,
                Map.of()));
        channel.setChannelExt(ext);

        NotifySendResult result = adapter.send(channel, new MessagePayload(null, "CARD", Map.of("title", "hello"),
                java.util.List.of()));

        assertThat(result.getStatusFlag()).isEqualTo(NotifyHistoryStatusEnum.FAILED);
        assertThat(result.getErrorMessage()).contains("secret");
    }

}
