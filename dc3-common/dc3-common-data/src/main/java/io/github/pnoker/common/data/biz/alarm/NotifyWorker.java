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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.dal.NotifyHistoryManager;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.entity.dto.NotifyTaskDTO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.utils.RabbitAckUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
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
 * @version 2025.9.0
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

    private final NotifyChannelManager notifyChannelManager;

    private final NotifyChannelBuilder notifyChannelBuilder;

    private final NotifyChannelAdapterRegistry notifyChannelAdapterRegistry;

    private final NotifyHistoryManager notifyHistoryManager;

    private final NotifyTaskSender notifyTaskSender;

    @RabbitHandler
    @RabbitListener(queues = "#{notifyTaskQueue.name}")
    public void onNotifyTask(Channel channel, Message message, NotifyTaskDTO task) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            if (Objects.isNull(task) || Objects.isNull(task.getNotifyHistoryId())
                    || Objects.isNull(task.getChannelId())) {
                log.error("Invalid notify task payload: {}", task);
                RabbitAckUtil.reject(channel, deliveryTag);
                return;
            }
            dispatch(task);
            RabbitAckUtil.ack(channel, deliveryTag);
        } catch (Exception e) {
            log.error("Notify task consume failed, historyId={}, channelId={}, retry={}",
                    Objects.nonNull(task) ? task.getNotifyHistoryId() : null,
                    Objects.nonNull(task) ? task.getChannelId() : null,
                    Objects.nonNull(task) ? task.getRetryCount() : 0, e);
            // Worker exception is internal — the task itself is preserved on the
            // history row and the next /admin replay can pick it up; nack-requeue
            // would loop in tight cases.
            RabbitAckUtil.nack(channel, deliveryTag, false);
        }
    }

    /**
     * Dispatch a notify task: resolve and validate the channel, find its adapter, send
     * the payload, then persist the terminal or retrying result.
     *
     * @param task the notify task to dispatch
     */
    private void dispatch(NotifyTaskDTO task) {
        NotifyChannelBO channel = loadChannel(task.getChannelId(), task.getTenantId());
        if (Objects.isNull(channel)) {
            persistTerminal(task, NotifySendResult.skipped(
                    "notify-channel" + SymbolConstant.COLON + task.getChannelId(),
                    "Notify channel not found or tenant mismatch"));
            return;
        }
        if (!EnableFlagEnum.ENABLE.equals(channel.getEnableFlag())) {
            persistTerminal(task, NotifySendResult.skipped(channel.getCredentialRef(), "Notify channel is disabled"));
            return;
        }
        NotifyChannelTypeEnum type = channel.getChannelTypeFlag();
        NotifyChannelAdapter adapter = notifyChannelAdapterRegistry.find(type).orElse(null);
        if (Objects.isNull(adapter)) {
            persistTerminal(task, NotifySendResult.failed(channel.getCredentialRef(),
                    "Notify channel adapter is missing for type=" + type));
            return;
        }

        MessagePayload payload = new MessagePayload(
                type,
                task.getPayloadType(),
                Objects.requireNonNullElse(task.getPayload(), Map.of()),
                Objects.requireNonNullElse(task.getMissingVariables(), List.of()));
        NotifySendResult result = adapter.send(channel, payload);
        if (NotifyHistoryStatusEnum.SUCCESS.equals(result.getStatusFlag())
                || NotifyHistoryStatusEnum.SKIPPED.equals(result.getStatusFlag())) {
            persistTerminal(task, result);
            return;
        }
        // FAILED — decide retry vs terminal.
        int nextAttempt = task.getRetryCount() + 1;
        if (nextAttempt >= MAX_ATTEMPTS) {
            persistTerminal(task, result);
            return;
        }
        persistRetrying(task, result, nextAttempt);
        NotifyTaskDTO retry = NotifyTaskDTO.builder()
                .notifyHistoryId(task.getNotifyHistoryId())
                .tenantId(task.getTenantId())
                .channelId(task.getChannelId())
                .channelTypeFlag(task.getChannelTypeFlag())
                .payloadType(task.getPayloadType())
                .payload(task.getPayload())
                .missingVariables(task.getMissingVariables())
                .retryCount(nextAttempt)
                .createTime(LocalDateTime.now())
                .build();
        notifyTaskSender.publish(retry);
    }

    /**
     * Load a notify channel by id, requiring it to belong to the tenant and carry a
     * channel type.
     *
     * @param channelId the channel id
     * @param tenantId  tenant scope
     * @return the channel, or null when missing, cross-tenant, or untyped
     */
    private NotifyChannelBO loadChannel(Long channelId, Long tenantId) {
        NotifyChannelDO entityDO = notifyChannelManager.getById(channelId);
        if (Objects.isNull(entityDO) || !Objects.equals(entityDO.getTenantId(), tenantId)
                || Objects.isNull(entityDO.getChannelTypeFlag())) {
            return null;
        }
        return notifyChannelBuilder.buildBOByDO(entityDO);
    }

    /**
     * Final outcome (SUCCESS / FAILED / SKIPPED). Updates the history row in
     * place — the row id was assigned when the PENDING row was inserted.
     */
    private void persistTerminal(NotifyTaskDTO task, NotifySendResult result) {
        NotifyHistoryDO update = new NotifyHistoryDO();
        update.setId(task.getNotifyHistoryId());
        update.setStatusFlag(result.getStatusFlag().getIndex());
        update.setTarget(Objects.toString(result.getTarget(), ""));
        update.setResponseExt(toResponseExt(result));
        update.setErrorMessage(Objects.toString(result.getErrorMessage(), ""));
        update.setRetryCount(task.getRetryCount());
        notifyHistoryManager.updateById(update);
    }

    /**
     * Retryable failure. Status flips to RETRYING and retry_count is bumped so
     * the dashboard reflects in-flight attempts.
     */
    private void persistRetrying(NotifyTaskDTO task, NotifySendResult result, int attempt) {
        NotifyHistoryDO update = new NotifyHistoryDO();
        update.setId(task.getNotifyHistoryId());
        update.setStatusFlag(NotifyHistoryStatusEnum.RETRYING.getIndex());
        update.setTarget(Objects.toString(result.getTarget(), ""));
        update.setResponseExt(toResponseExt(result));
        update.setErrorMessage(Objects.toString(result.getErrorMessage(), ""));
        update.setRetryCount(attempt);
        notifyHistoryManager.updateById(update);
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
