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
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Generic webhook notification channel adapter.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
public class WebhookNotifyChannelAdapter implements NotifyChannelAdapter {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    protected final OkHttpClient okHttpClient;

    protected final NotifyCredentialResolver notifyCredentialResolver;

    public WebhookNotifyChannelAdapter(OkHttpClient okHttpClient, NotifyCredentialResolver notifyCredentialResolver) {
        this.okHttpClient = okHttpClient;
        this.notifyCredentialResolver = notifyCredentialResolver;
    }

    @Override
    public NotifyChannelTypeEnum channelType() {
        return NotifyChannelTypeEnum.WEBHOOK;
    }

    @Override
    public NotifySendResult send(NotifyChannelBO channel, MessagePayload payload) {
        Optional<NotifyCredential> credentialOptional = notifyCredentialResolver.resolve(channel.getCredentialRef());
        if (credentialOptional.isEmpty() || StringUtils.isBlank(credentialOptional.get().getWebhookUrl())) {
            return NotifySendResult.failed(channel.getCredentialRef(), "Notify credential is not configured");
        }

        NotifyCredential credential = credentialOptional.get();
        return postJson(channel.getCredentialRef(), credential, payload.getPayload());
    }

    protected NotifySendResult postJson(String target, NotifyCredential credential, Map<String, Object> body) {
        String json = JsonUtil.toJsonString(body);
        Request.Builder builder = new Request.Builder()
                .url(credential.getWebhookUrl())
                .post(RequestBody.create(json, JSON));
        for (Map.Entry<String, String> header : credential.getHeaders().entrySet()) {
            builder.header(header.getKey(), header.getValue());
        }

        try (Response response = okHttpClient.newCall(builder.build()).execute()) {
            Map<String, Object> responsePayload = new LinkedHashMap<>();
            responsePayload.put("code", response.code());
            responsePayload.put("message", response.message());
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                responsePayload.put("body", responseBody.string());
            }
            if (response.isSuccessful()) {
                return NotifySendResult.success(target, response.code(), response.message(), responsePayload);
            }
            return new NotifySendResult(NotifyHistoryStatusEnum.FAILED, target, response.code(), response.message(),
                    null, responsePayload, response.message());
        } catch (IOException e) {
            return NotifySendResult.failed(target, e.getMessage());
        }
    }

}
