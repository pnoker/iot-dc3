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

import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.service.ModelConfigService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelControllerTest {

    @Mock
    private ModelConfigBuilder modelConfigBuilder;

    @Mock
    private ModelConfigService modelConfigService;

    private ModelController controller;

    @BeforeEach
    void setUp() {
        controller = new ModelController(modelConfigBuilder, modelConfigService);
    }

    @Test
    void listForwardsListOptionsFromService() {
        ModelVO option = new ModelVO("gpt-4o", "GPT 4o", true, true, true, false, 0.7, 2048);
        RequestHeader.PrincipalHeader header = header();
        when(modelConfigService.listOptions(header)).thenReturn(Mono.just(List.of(option)));

        StepVerifier.create(controller.list()
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new GatewayAuthenticationToken(header, List.of()))))
                .assertNext(response -> {
                    assertThat(response).containsExactly(option);
                })
                .expectComplete()
                .verify();
    }

    @Test
    void deleteCallsServiceAndCompletesWithoutBody() {
        RequestHeader.PrincipalHeader header = header();
        when(modelConfigService.delete(42L, header)).thenReturn(Mono.empty());
        StepVerifier.create(controller.delete(42L)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                new GatewayAuthenticationToken(header, List.of()))))
                .verifyComplete();
        verify(modelConfigService).delete(42L, header);
    }

    private RequestHeader.PrincipalHeader header() {
        RequestHeader.PrincipalHeader header = new RequestHeader.PrincipalHeader();
        header.setTenantId(1L);
        header.setPrincipalId(2L);
        header.setPrincipalName("admin");
        return header;
    }
}
