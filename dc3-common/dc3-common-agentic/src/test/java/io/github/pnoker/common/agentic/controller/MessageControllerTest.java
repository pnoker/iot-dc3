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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.builder.MessageBuilder;
import io.github.pnoker.common.agentic.entity.dto.ConversationPageQueryDTO;
import io.github.pnoker.common.agentic.entity.vo.MessageVO;
import io.github.pnoker.common.agentic.service.MessageService;
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
 * Verifies the message list contract: POST-body pagination with platform bounds
 * (default 50, cap 200, non-negative offset) and controller-to-service layering.
 */
@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    @Mock
    private MessageBuilder messageBuilder;

    @Mock
    private MessageService messageService;

    private MessageController controller;

    private RequestHeader.PrincipalHeader header;

    @BeforeEach
    void setUp() {
        controller = new MessageController(messageBuilder, messageService);
        header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
    }

    @Test
    void pagesMessagesThroughTheServiceWithDefaults() {
        MessageBO messageBO = new MessageBO();
        MessageVO messageVO = new MessageVO();
        when(messageService.list(0L, 50, "conv-1", header))
                .thenReturn(Mono.just(OffsetPage.of(List.of(messageBO), 0L, 50, 120)));
        when(messageBuilder.buildVOByBO(any(MessageBO.class))).thenReturn(messageVO);

        StepVerifier.create(controller
                        .list(new ConversationPageQueryDTO("conv-1", null, null))
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(messageVO);
                    assertThat(page.offset()).isZero();
                    assertThat(page.limit()).isEqualTo(50);
                    assertThat(page.total()).isEqualTo(120);
                    assertThat(page.hasNext()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void rejectsLimitAboveTheCapBeforeTouchingTheService() {
        StepVerifier.create(controller
                        .list(new ConversationPageQueryDTO("conv-1", 0L, 201))
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(messageService);
    }

    @Test
    void rejectsNegativeOffsetBeforeTouchingTheService() {
        StepVerifier.create(controller
                        .list(new ConversationPageQueryDTO("conv-1", -1L, 50))
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication())))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(messageService);
    }

    @Test
    void rejectsMissingConversationIdBeforeTouchingTheService() {
        StepVerifier.create(controller.list(new ConversationPageQueryDTO(null, 0L, 50)))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(messageService);
    }

    @Test
    void toleratesAbsentBodyWithValidationStillEnforced() {
        StepVerifier.create(controller.list(null))
                .expectError(RequestException.class)
                .verify();
        verifyNoInteractions(messageService);
    }

    private GatewayAuthenticationToken authentication() {
        return new GatewayAuthenticationToken(header, List.of());
    }
}
