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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.agentic.entity.builder.AttachmentBuilder;
import io.github.pnoker.common.agentic.service.AttachmentService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    @Mock
    private AttachmentBuilder attachmentBuilder;

    @Mock
    private AttachmentService attachmentService;

    private AttachmentController controller;

    @BeforeEach
    void setUp() {
        controller = new AttachmentController(attachmentBuilder, attachmentService);
    }

    @Test
    void deleteForwardsIdAndPrincipalToService() {
        RequestHeader.PrincipalHeader header = header();
        when(attachmentService.delete(42L, header)).thenReturn(Mono.just(1L));

        StepVerifier.create(controller
                        .delete(42L)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new GatewayAuthenticationToken(header, List.of()))))
                .verifyComplete();
        verify(attachmentService).delete(42L, header);
    }

    @Test
    void deleteWithoutPrincipalErrorsBeforeTouchingTheService() {
        StepVerifier.create(controller.delete(42L)).expectError(UnAuthorizedException.class).verify();
        verifyNoInteractions(attachmentService);
    }

    @Test
    void deletePropagatesServiceNotFound() {
        RequestHeader.PrincipalHeader header = header();
        when(attachmentService.delete(42L, header))
                .thenReturn(Mono.error(new NotFoundException("Attachment does not exist")));

        StepVerifier.create(controller
                        .delete(42L)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new GatewayAuthenticationToken(header, List.of()))))
                .expectError(NotFoundException.class)
                .verify();
    }

    private RequestHeader.PrincipalHeader header() {
        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
        return header;
    }
}
