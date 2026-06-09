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
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Feishu bot notification channel adapter.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
public class FeishuBotNotifyChannelAdapter extends WebhookNotifyChannelAdapter {

    private static final String HMAC_SHA256 = "HmacSHA256";

    public FeishuBotNotifyChannelAdapter(OkHttpClient okHttpClient,
                                         NotifyCredentialResolver notifyCredentialResolver) {
        super(okHttpClient, notifyCredentialResolver);
    }

    @Override
    public NotifyChannelTypeEnum channelType() {
        return NotifyChannelTypeEnum.FEISHU_BOT;
    }

    @Override
    public NotifySendResult send(NotifyChannelBO channel, MessagePayload payload) {
        Optional<NotifyCredential> credentialOptional = notifyCredentialResolver.resolve(channel.getCredentialRef());
        if (credentialOptional.isEmpty() || StringUtils.isBlank(credentialOptional.get().getWebhookUrl())) {
            return NotifySendResult.failed(channel.getCredentialRef(), "Notify credential is not configured");
        }

        NotifyCredential credential = credentialOptional.get();
        Map<String, Object> body;
        try {
            body = buildFeishuBody(channel, credential, payload);
        } catch (IllegalStateException e) {
            return NotifySendResult.failed(channel.getCredentialRef(), e.getMessage());
        }
        if (body.containsKey("error")) {
            return NotifySendResult.failed(channel.getCredentialRef(), Objects.toString(body.get("error")));
        }
        return postJson(channel.getCredentialRef(), credential, body);
    }

    private Map<String, Object> buildFeishuBody(NotifyChannelBO channel, NotifyCredential credential,
                                                MessagePayload payload) {
        Map<String, Object> body = buildFeishuMessage(payload);
        if (signEnabled(channel)) {
            if (StringUtils.isBlank(credential.getSecret())) {
                return Map.of("error", "Feishu bot signing is enabled but secret is not configured");
            }
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            body.put("timestamp", timestamp);
            body.put("sign", sign(timestamp, credential.getSecret()));
        }
        return body;
    }

    private Map<String, Object> buildFeishuMessage(MessagePayload payload) {
        Map<String, Object> renderedPayload = Objects.requireNonNullElse(payload.getPayload(), Map.of());
        if (renderedPayload.containsKey("msg_type")) {
            return new LinkedHashMap<>(renderedPayload);
        }

        String payloadType = StringUtils.upperCase(payload.getPayloadType());
        if ("TEXT".equals(payloadType)) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msg_type", "text");
            body.put("content", Map.of("text", textContent(renderedPayload)));
            return body;
        }
        if ("JSON".equals(payloadType)) {
            return new LinkedHashMap<>(renderedPayload);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("msg_type", "interactive");
        body.put("card", renderedPayload);
        return body;
    }

    private String textContent(Map<String, Object> payload) {
        Object text = payload.get("text");
        if (Objects.nonNull(text)) {
            return Objects.toString(text);
        }
        Object summary = payload.get("summary");
        if (Objects.nonNull(summary)) {
            return Objects.toString(summary);
        }
        return Objects.toString(payload.getOrDefault("title", ""));
    }

    private boolean signEnabled(NotifyChannelBO channel) {
        return Objects.nonNull(channel.getChannelExt())
                && Objects.nonNull(channel.getChannelExt().getContent())
                && Boolean.TRUE.equals(channel.getChannelExt().getContent().getSignEnabled());
    }

    private String sign(String timestamp, String secret) {
        try {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign Feishu bot request", e);
        }
    }

}
