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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Generic webhook notification channel adapter.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Service
public class WebhookNotifyChannelAdapter implements NotifyChannelAdapter {

    protected final WebClient.Builder webClientBuilder;

    protected final NotifyCredentialResolver notifyCredentialResolver;

    public WebhookNotifyChannelAdapter(
            WebClient.Builder webClientBuilder, NotifyCredentialResolver notifyCredentialResolver) {
        this.webClientBuilder = webClientBuilder;
        this.notifyCredentialResolver = notifyCredentialResolver;
    }

    @Override
    public NotifyChannelTypeEnum channelType() {
        return NotifyChannelTypeEnum.WEBHOOK;
    }

    @Override
    public Mono<NotifySendResult> send(NotifyChannelBO channel, MessagePayload payload) {
        Optional<NotifyCredential> credentialOptional = notifyCredentialResolver.resolve(channel.getCredentialRef());
        if (credentialOptional.isEmpty()
                || StringUtils.isBlank(credentialOptional.get().getWebhookUrl())) {
            return Mono.just(
                    NotifySendResult.failed(channel.getCredentialRef(), "Notify credential is not configured"));
        }

        NotifyCredential credential = credentialOptional.get();
        return postJson(channel.getCredentialRef(), credential, payload.getPayload());
    }

    /**
     * Post json.
     *
     * @param target     target
     * @param credential credential
     * @param body       body
     * @return asynchronous post json result
     */
    protected Mono<NotifySendResult> postJson(String target, NotifyCredential credential, Map<String, Object> body) {
        return webClientBuilder
                .build()
                .post()
                .uri(credential.getWebhookUrl())
                .headers(headers -> headers.setAll(credential.getHeaders()))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchangeToMono(response -> response.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .map(responseBody -> {
                            Map<String, Object> responsePayload = new LinkedHashMap<>();
                            responsePayload.put("code", response.statusCode().value());
                            responsePayload.put("message", response.statusCode().toString());
                            responsePayload.put("body", responseBody);
                            if (response.statusCode().is2xxSuccessful()) {
                                return NotifySendResult.success(
                                        target,
                                        response.statusCode().value(),
                                        response.statusCode().toString(),
                                        responsePayload);
                            }
                            return new NotifySendResult(
                                    NotifyHistoryStatusEnum.FAILED,
                                    target,
                                    response.statusCode().value(),
                                    response.statusCode().toString(),
                                    null,
                                    responsePayload,
                                    responseBody);
                        }))
                .onErrorResume(error -> Mono.just(NotifySendResult.failed(target, error.getMessage())));
    }
}
