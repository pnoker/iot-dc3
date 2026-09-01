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

import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.constant.mq.MqTopic;
import io.github.pnoker.common.data.repository.ReactiveNotifyHistoryStore;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.mq.annotation.Dc3Listener;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Consumes {@link NotifyTaskDTO} payloads from {@code dc3.q.notify.task},
 * dispatches them through the matching {@link NotifyChannelAdapter}, and
 * stamps the corresponding {@code dc3_notify_history} row with the outcome.
 *
 * <p>Retry semantics: a failure increments the task's retry count and either
 * (a) republishes a fresh copy of the task — recording RETRYING on the
 * history row — until the max attempt count is exhausted, or (b) terminates
 * with FAILED and acks. We deliberately avoid {@code basicNack(requeue=true)}
 * because RabbitMQ would put the message back at the head of the queue and
 * the worker would loop on it tightly; re-publishing puts it at the tail.
 *
 * <p>No dead-letter queue is wired — terminal FAILED rows in
 * {@code dc3_notify_history} are the audit trail for operators.
 *
 * @author pnoker
 * @since 2026.5.21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyWorker {

    /**
     * Maximum dispatch attempts (initial + retries). Anything past this is
     * marked FAILED rather than re-queued.
     */
    public static final int MAX_ATTEMPTS = 3;

    private final NotifyConfigCache notifyConfigCache;

    private final NotifyChannelAdapterRegistry notifyChannelAdapterRegistry;

    private final ReactiveNotifyHistoryStore notifyHistoryStore;

    private final NotifyTaskSender notifyTaskSender;

    /**
     * On notify task.
     *
     * @param message the notify task delivery
     * @param ack     the acknowledgement handle
     */
    @Dc3Listener(topic = MqTopic.NOTIFY_TASK)
    public Mono<Void> onNotifyTask(MqReceived<NotifyTaskDTO> message, Acknowledgment ack) {
        NotifyTaskDTO task = message.payload();
        if (Objects.isNull(task) || Objects.isNull(task.getNotifyHistoryId())
                || Objects.isNull(task.getChannelId()) || Objects.isNull(task.getTenantId())
                || task.getTenantId() <= 0) {
            log.error("Notify task rejected, reason=invalidEnvelope, historyId={}, channelId={}",
                    Objects.nonNull(task) ? task.getNotifyHistoryId() : null,
                    Objects.nonNull(task) ? task.getChannelId() : null);
            ack.reject(false);
            return Mono.empty();
        }
        return dispatch(task)
                .doOnError(error -> log.error("Notify task persistence failed, historyId={}",
                        task.getNotifyHistoryId(), error));
    }

    /**
     * Dispatch a notify task: resolve and validate the channel, find its adapter, send
     * the payload, then persist the terminal or retrying result.
     *
     * @param task the notify task to dispatch
     */
    private Mono<Void> dispatch(NotifyTaskDTO task) {
        return notifyConfigCache.getChannel(task.getChannelId(), task.getTenantId())
                .switchIfEmpty(Mono.error(new MissingNotifyChannelException(task.getChannelId())))
                .flatMap(channel -> dispatch(channel, task))
                .onErrorResume(MissingNotifyChannelException.class, error -> persistTerminal(task,
                        NotifySendResult.skipped("notify-channel" + SymbolConstant.COLON + task.getChannelId(),
                                "Notify channel not found or tenant mismatch")));
    }

    private Mono<Void> dispatch(NotifyChannelBO channel, NotifyTaskDTO task) {
        if (!EnableFlagEnum.ENABLE.equals(channel.getEnableFlag())) {
            return persistTerminal(task, NotifySendResult.skipped(channel.getCredentialRef(), "Notify channel is disabled"));
        }
        NotifyChannelTypeEnum type = channel.getChannelTypeFlag();
        NotifyChannelAdapter adapter = notifyChannelAdapterRegistry.find(type).orElse(null);
        if (Objects.isNull(adapter)) {
            return persistTerminal(task, NotifySendResult.failed(channel.getCredentialRef(),
                    "Notify channel adapter is missing for type=" + type));
        }

        MessagePayload payload = new MessagePayload(type, task.getPayloadType(),
                Objects.requireNonNullElse(task.getPayload(), Map.of()),
                Objects.requireNonNullElse(task.getMissingVariables(), List.of()));
        return Mono.defer(() -> adapter.send(channel, payload))
                .flatMap(result -> {
                    if (NotifyHistoryStatusEnum.SUCCESS.equals(result.getStatusFlag())
                            || NotifyHistoryStatusEnum.SKIPPED.equals(result.getStatusFlag())) {
                        return persistTerminal(task, result);
                    }
                    int nextAttempt = Objects.requireNonNullElse(task.getRetryCount(), 0) + 1;
                    if (nextAttempt >= MAX_ATTEMPTS) {
                        return persistTerminal(task, result);
                    }
                    return persistRetrying(task, result, nextAttempt).then(Mono.defer(() -> {
                        NotifyTaskDTO retry = NotifyTaskDTO.builder()
                .notifyHistoryId(task.getNotifyHistoryId())
                .tenantId(task.getTenantId())
                .channelId(task.getChannelId())
                .channelTypeFlag(task.getChannelTypeFlag())
                .payloadType(task.getPayloadType())
                .payload(task.getPayload())
                .missingVariables(task.getMissingVariables())
                .retryCount(nextAttempt)
                .createTime(LocalDateTime.now(ZoneOffset.UTC))
                .build();
                        Mono<Void> publication = notifyTaskSender.publish(retry);
                        return publication == null ? Mono.empty() : publication;
                    }));
                });
    }

    private static final class MissingNotifyChannelException extends RuntimeException {
        private MissingNotifyChannelException(Long channelId) {
            super("Notify channel not found: " + channelId);
        }
    }

    /**
     * Final outcome (SUCCESS / FAILED / SKIPPED). Updates the history row in
     * place — the row id was assigned when the PENDING row was inserted.
     */
    private Mono<Void> persistTerminal(NotifyTaskDTO task, NotifySendResult result) {
        return notifyHistoryStore.updateDelivery(task.getTenantId(), task.getNotifyHistoryId(), result.getStatusFlag().getIndex(),
                        Objects.toString(result.getTarget(), ""), toResponseExt(result),
                        Objects.toString(result.getErrorMessage(), ""), task.getRetryCount())
                .flatMap(updated -> updated ? Mono.<Void>empty() : Mono.error(new IllegalStateException("notify history row not found")));
    }

    /**
     * Retryable failure. Status flips to RETRYING and retry_count is bumped so
     * the dashboard reflects in-flight attempts.
     */
    private Mono<Void> persistRetrying(NotifyTaskDTO task, NotifySendResult result, int attempt) {
        return notifyHistoryStore.updateDelivery(task.getTenantId(), task.getNotifyHistoryId(), NotifyHistoryStatusEnum.RETRYING.getIndex(),
                        Objects.toString(result.getTarget(), ""), toResponseExt(result),
                        Objects.toString(result.getErrorMessage(), ""), attempt)
                .flatMap(updated -> updated ? Mono.<Void>empty() : Mono.error(new IllegalStateException("notify history row not found")));
    }

    private JsonExt toResponseExt(NotifySendResult result) {
        NotifyHistoryResponseExt ext = new NotifyHistoryResponseExt();
        ext.setType("ALARM_NOTIFY_HISTORY_RESPONSE");
        ext.setVersion(1);
        ext.setContent(new NotifyHistoryResponseExt.Content(
                result.getProviderMessageId(),
                result.getStatusCode(),
                result.getStatusMessage(),
                Objects.requireNonNullElse(result.getResponsePayload(), Map.of())));
        return JsonExt.builder()
                .type(ext.getType())
                .version(ext.getVersion())
                .content(result.getStatusMessage() == null ? "" : result.getStatusMessage())
                .remark(io.github.pnoker.common.utils.JsonUtil.toJsonString(ext.getContent()))
                .build();
    }

}
