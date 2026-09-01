/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */
package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.repository.ReactiveActionStore;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.common.utils.UuidV7;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

/** Implements the agentic action lifecycle. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private static final String ACTION_WRITE_POINT_VALUE = "writePointValue";

    private final ReactiveActionStore actionStore;
    private final PointCommandFacade pointCommandFacade;

    @Override
    public Mono<String> createWritePointValueAction(String conversationId, Long deviceId, Long pointId,
                                                    String value, RequestHeader.PrincipalHeader header) {
        Objects.requireNonNull(header, "header must not be null");
        if (conversationId == null || conversationId.isBlank()) {
            return Mono.error(new RequestException("Conversation ID is required"));
        }
        if (deviceId == null || pointId == null) {
            return Mono.error(new RequestException("Device ID and point ID are required"));
        }
        ActionBO action = new ActionBO();
        action.setActionId(UuidV7.next().toString());
        action.setConversationId(conversationId);
        action.setActionType(ACTION_WRITE_POINT_VALUE);
        action.setTitle("Write point value");
        action.setDescription("Write value to device " + deviceId + ", point " + pointId);
        action.setPayload(Map.of("deviceId", deviceId, "pointId", pointId, "value", value));
        action.setStatus(AgenticActionStatusEnum.PENDING);
        action.setExpireTime(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        action.setTenantId(header.getTenantId());
        action.setUserId(header.getUserId());
        fillCreateAudit(action, header);
        return actionStore.create(action).map(ActionBO::getActionId);
    }


    @Override
    public Mono<OffsetPage<ActionBO>> listPending(long offset, int limit, String conversationId,
                                                   RequestHeader.PrincipalHeader header) {
        Objects.requireNonNull(header, "header must not be null");
        return actionStore.listPending(offset, limit, conversationId, header, Instant.now());
    }

    @Override
    public Mono<ActionBO> confirm(String actionId, RequestHeader.PrincipalHeader header) {
        return getPending(actionId, header)
                .flatMap(action -> actionStore.claimPending(actionId, header,
                                AgenticActionStatusEnum.CONFIRMED, Instant.now())
                        .switchIfEmpty(Mono.error(new RequestException("Agentic action is no longer pending"))))
                .flatMap(action -> execute(action, header));
    }

    @Override
    public Mono<ActionBO> reject(String actionId, RequestHeader.PrincipalHeader header) {
        return getPending(actionId, header)
                .flatMap(action -> actionStore.claimPending(actionId, header,
                                AgenticActionStatusEnum.REJECTED, Instant.now())
                        .switchIfEmpty(Mono.error(new RequestException("Agentic action is no longer pending"))));
    }

    private Mono<ActionBO> getPending(String actionId, RequestHeader.PrincipalHeader header) {
        if (actionId == null || actionId.isBlank()) {
            return Mono.error(new RequestException("Action ID is required"));
        }
        if (header == null) {
            return Mono.error(new IllegalArgumentException("header must not be null"));
        }
        return actionStore.find(actionId, header)
                .switchIfEmpty(Mono.error(new NotFoundException("Agentic action does not exist")))
                .flatMap(action -> {
                    if (action.getStatus() != AgenticActionStatusEnum.PENDING) {
                        return Mono.error(new RequestException("Agentic action is no longer pending"));
                    }
                    if (action.getExpireTime() != null
                            && action.getExpireTime().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
                        return Mono.error(new RequestException("Agentic action has expired"));
                    }
                    return Mono.just(action);
                });
    }

    private Mono<ActionBO> execute(ActionBO action, RequestHeader.PrincipalHeader header) {
        if (!ACTION_WRITE_POINT_VALUE.equals(action.getActionType())) {
            return actionStore.updateExecutionResult(action.getActionId(), header,
                    AgenticActionStatusEnum.FAILED, "Unsupported action type", Instant.now());
        }
        Map<String, Object> payload = action.getPayload();
        return pointCommandFacade.submitWrite(header.getTenantId(), longValue(payload.get("deviceId")),
                        longValue(payload.get("pointId")), Objects.toString(payload.get("value"), ""),
                        PointCommandSourceEnum.AGENTIC)
                .flatMap(commandId -> actionStore.updateExecutionResult(action.getActionId(), header,
                        AgenticActionStatusEnum.EXECUTED, "Command accepted: " + commandId, Instant.now()))
                .onErrorResume(exception -> {
                    log.warn("Action execution failed, actionId={}", action.getActionId(), exception);
                    return actionStore.updateExecutionResult(action.getActionId(), header,
                            AgenticActionStatusEnum.FAILED, exception.getMessage(), Instant.now());
                });
    }

    private void fillCreateAudit(ActionBO entityBO, RequestHeader.PrincipalHeader header) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value != null) {
            try {
                return Long.valueOf(value.toString());
            } catch (NumberFormatException exception) {
                throw new RequestException("Agentic action payload ID is not a valid number", exception);
            }
        }
        throw new RequestException("Agentic action payload is missing required ID");
    }
}
