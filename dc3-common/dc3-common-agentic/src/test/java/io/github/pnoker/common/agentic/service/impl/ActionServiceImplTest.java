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
package io.github.pnoker.common.agentic.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.repository.ReactiveActionStore;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.enums.PointCommandSourceEnum;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

/**
 * Unit coverage of the reactive action lifecycle.
 */
@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock
    private ReactiveActionStore actionStore;

    @Mock
    private PointCommandFacade pointCommandFacade;

    private ActionServiceImpl service;
    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        service = new ActionServiceImpl(actionStore, pointCommandFacade);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void reactiveCreatePersistsThroughActionStore() {
        ActionBO persisted = pendingAction();
        when(actionStore.create(any(ActionBO.class))).thenReturn(reactor.core.publisher.Mono.just(persisted));

        StepVerifier.create(service.createWritePointValueAction("conv", 10L, 20L, "42", header))
                .expectNext("action-1")
                .verifyComplete();

        ArgumentCaptor<ActionBO> captor = ArgumentCaptor.forClass(ActionBO.class);
        verify(actionStore).create(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(AgenticActionStatusEnum.PENDING);
        assertThat(captor.getValue().getTenantId()).isEqualTo(1L);
        assertThat(captor.getValue().getUserId()).isEqualTo(2L);
    }

    @Test
    void listPendingDelegatesTenantScopedQueryToReactiveStore() {
        ActionBO action = pendingAction();
        when(actionStore.listPending(eq(0L), eq(50), eq("conv"), eq(header), any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.just(OffsetPage.of(java.util.List.of(action), 0, 50, 1)));

        StepVerifier.create(service.listPending(0, 50, "conv", header))
                .assertNext(page -> assertThat(page.items()).containsExactly(action))
                .verifyComplete();

        verify(actionStore).listPending(eq(0L), eq(50), eq("conv"), eq(header), any(Instant.class));
    }

    @Test
    void confirmClaimsAndPersistsExecutedResult() {
        ActionBO action = pendingAction();
        ActionBO claimed = pendingAction();
        claimed.setStatus(AgenticActionStatusEnum.CONFIRMED);
        ActionBO executed = pendingAction();
        executed.setStatus(AgenticActionStatusEnum.EXECUTED);
        when(actionStore.find("action-1", header)).thenReturn(reactor.core.publisher.Mono.just(action));
        when(actionStore.claimPending(
                        eq("action-1"), eq(header), eq(AgenticActionStatusEnum.CONFIRMED), any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.just(claimed));
        when(pointCommandFacade.submitWrite(1L, 10L, 20L, "42", PointCommandSourceEnum.AGENTIC))
                .thenReturn(reactor.core.publisher.Mono.just("cmd-1"));
        when(actionStore.updateExecutionResult(
                        eq("action-1"),
                        eq(header),
                        eq(AgenticActionStatusEnum.EXECUTED),
                        eq("Command accepted: cmd-1"),
                        any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.just(executed));

        StepVerifier.create(service.confirm("action-1", header))
                .expectNext(executed)
                .verifyComplete();
    }

    @Test
    void confirmRaceReturnsRequestErrorAndDoesNotDispatchCommand() {
        ActionBO action = pendingAction();
        when(actionStore.find("action-1", header)).thenReturn(reactor.core.publisher.Mono.just(action));
        when(actionStore.claimPending(
                        eq("action-1"), eq(header), eq(AgenticActionStatusEnum.CONFIRMED), any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.empty());

        StepVerifier.create(service.confirm("action-1", header))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(RequestException.class)
                        .hasMessage("Agentic action is no longer pending"))
                .verify();

        verify(pointCommandFacade, never()).submitWrite(any(), any(), any(), any());
    }

    @Test
    void rejectExpiredActionDoesNotClaim() {
        ActionBO action = pendingAction();
        action.setExpireTime(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(actionStore.find("action-1", header)).thenReturn(reactor.core.publisher.Mono.just(action));

        StepVerifier.create(service.reject("action-1", header))
                .expectErrorSatisfies(error ->
                        assertThat(error).isInstanceOf(RequestException.class).hasMessage("Agentic action has expired"))
                .verify();

        verify(actionStore, never()).claimPending(any(), any(), any(), any());
    }

    @Test
    void facadeFailureIsPersistedAsFailedAction() {
        ActionBO action = pendingAction();
        ActionBO claimed = pendingAction();
        claimed.setStatus(AgenticActionStatusEnum.CONFIRMED);
        ActionBO failed = pendingAction();
        failed.setStatus(AgenticActionStatusEnum.FAILED);
        when(actionStore.find("action-1", header)).thenReturn(reactor.core.publisher.Mono.just(action));
        when(actionStore.claimPending(
                        eq("action-1"), eq(header), eq(AgenticActionStatusEnum.CONFIRMED), any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.just(claimed));
        when(pointCommandFacade.submitWrite(1L, 10L, 20L, "42", PointCommandSourceEnum.AGENTIC))
                .thenReturn(reactor.core.publisher.Mono.error(new IllegalStateException("broker unavailable")));
        when(actionStore.updateExecutionResult(
                        eq("action-1"),
                        eq(header),
                        eq(AgenticActionStatusEnum.FAILED),
                        eq("broker unavailable"),
                        any(Instant.class)))
                .thenReturn(reactor.core.publisher.Mono.just(failed));

        StepVerifier.create(service.confirm("action-1", header))
                .expectNext(failed)
                .verifyComplete();
    }

    private ActionBO pendingAction() {
        ActionBO action = new ActionBO();
        action.setActionId("action-1");
        action.setConversationId("conv");
        action.setActionType("writePointValue");
        action.setPayload(Map.of("deviceId", 10L, "pointId", 20L, "value", "42"));
        action.setStatus(AgenticActionStatusEnum.PENDING);
        action.setExpireTime(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        action.setTenantId(1L);
        action.setUserId(2L);
        return action;
    }
}
