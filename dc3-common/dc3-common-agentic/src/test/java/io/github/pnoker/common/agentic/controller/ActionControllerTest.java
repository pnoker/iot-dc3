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
package io.github.pnoker.common.agentic.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.entity.builder.ActionBuilder;
import io.github.pnoker.common.agentic.entity.dto.ConversationPageQueryDTO;
import io.github.pnoker.common.agentic.entity.vo.ActionVO;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import io.github.pnoker.db.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Verifies the pending-action list contract after the pagination unification: the
 * endpoint takes a POST body with platform pagination bounds and forwards the slice
 * to the service unchanged.
 */
@ExtendWith(MockitoExtension.class)
class ActionControllerTest {

    @Mock
    private ActionBuilder actionBuilder;

    @Mock
    private ActionService actionService;

    private ActionController controller;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        controller = new ActionController(actionBuilder, actionService);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void pagesPendingActionsThroughTheService() {
        ActionBO actionBO = new ActionBO();
        ActionVO actionVO = new ActionVO();
        when(actionService.listPending(10L, 20, "conv-1", header))
                .thenReturn(Mono.just(OffsetPage.of(List.of(actionBO), 10L, 20, 31)));
        when(actionBuilder.buildVOListByBOList(List.of(actionBO))).thenReturn(List.of(actionVO));

        StepVerifier.create(controller
                        .pending(new ConversationPageQueryDTO("conv-1", 10L, 20))
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(actionVO);
                    assertThat(page.offset()).isEqualTo(10L);
                    assertThat(page.limit()).isEqualTo(20);
                    assertThat(page.total()).isEqualTo(31);
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void rejectsLimitAboveTheCapBeforeTouchingTheService() {
        StepVerifier.create(controller
                        .pending(new ConversationPageQueryDTO("conv-1", 0L, 500))
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(actionService);
    }

    @Test
    void rejectsMissingConversationIdBeforeTouchingTheService() {
        StepVerifier.create(controller.pending(null))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(actionService);
    }

    private GatewayAuthenticationToken authentication() {
        return new GatewayAuthenticationToken(header, List.of());
    }
}
