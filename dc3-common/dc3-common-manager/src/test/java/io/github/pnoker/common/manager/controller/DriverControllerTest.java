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
package io.github.pnoker.common.manager.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.query.DriverListRequest;
import io.github.pnoker.common.manager.entity.vo.DriverVO;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.common.security.GatewayAuthenticationToken;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DriverControllerTest {

    private static final long TENANT_ID = 100L;

    @Mock
    private DriverBuilder driverBuilder;

    @Mock
    private ReactiveDriverService reactiveDriverService;

    private DriverController controller;

    @BeforeEach
    void setUp() {
        controller = new DriverController(driverBuilder, reactiveDriverService);
    }

    private static <T> Mono<T> withTenant(Mono<T> source) {
        RequestHeader.PrincipalHeader header =
                new RequestHeader.PrincipalHeader(7L, "USER", "Alice", "alice", TENANT_ID, null, null);
        return source.contextWrite(
                ReactiveSecurityContextHolder.withAuthentication(new GatewayAuthenticationToken(header, Set.of())));
    }

    @Test
    void addReturnsCreatedDriverAndInjectsTenant() {
        DriverVO request = new DriverVO();
        DriverBO input = new DriverBO();
        DriverBO saved = new DriverBO();
        DriverVO output = new DriverVO();
        when(driverBuilder.buildBOByVO(request)).thenReturn(input);
        when(reactiveDriverService.add(input)).thenReturn(Mono.just(saved));
        when(driverBuilder.buildVOByBO(saved)).thenReturn(output);
        StepVerifier.create(withTenant(controller.add(request)))
                .assertNext(response -> {
                    assertThat(response.getStatusCode().value()).isEqualTo(201);
                    assertThat(response.getBody()).isSameAs(output);
                })
                .verifyComplete();
        assertThat(input.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void deleteReturnsNoContent() {
        when(reactiveDriverService.delete(TENANT_ID, 1L, 3, 7L, "alice")).thenReturn(Mono.just(true));
        StepVerifier.create(withTenant(controller.delete(1L, 3)))
                .assertNext(
                        response -> assertThat(response.getStatusCode().value()).isEqualTo(204))
                .verifyComplete();
    }

    @Test
    void updateReturnsUpdatedDriver() {
        DriverVO request = new DriverVO();
        DriverBO input = new DriverBO();
        DriverBO saved = new DriverBO();
        DriverVO output = new DriverVO();
        when(driverBuilder.buildBOByVO(request)).thenReturn(input);
        when(reactiveDriverService.update(input)).thenReturn(Mono.just(saved));
        when(driverBuilder.buildVOByBO(saved)).thenReturn(output);
        StepVerifier.create(withTenant(controller.update(request)))
                .assertNext(response -> assertThat(response.getBody()).isSameAs(output))
                .verifyComplete();
        assertThat(input.getTenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void listUsesCanonicalOffsetPageAndTenant() {
        DriverBO driver = new DriverBO();
        DriverVO vo = new DriverVO();
        when(reactiveDriverService.list(any())).thenReturn(Mono.just(OffsetPage.of(List.of(driver), 20, 10, 31)));
        when(driverBuilder.buildVOByBO(driver)).thenReturn(vo);
        StepVerifier.create(withTenant(controller.list(new DriverListRequest(
                        20, 10, List.of(), null, null, null, null, null, null, null, null, null))))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(vo);
                    assertThat(page.offset()).isEqualTo(20);
                    assertThat(page.total()).isEqualTo(31);
                })
                .verifyComplete();
        ArgumentCaptor<io.github.pnoker.common.manager.repository.DriverFilter> captor =
                ArgumentCaptor.forClass(io.github.pnoker.common.manager.repository.DriverFilter.class);
        verify(reactiveDriverService).list(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(TENANT_ID);
    }

    @Test
    void listByIdsReturnsTenantScopedMap() {
        DriverBO driver = new DriverBO();
        driver.setId(1L);
        DriverVO vo = new DriverVO();
        when(reactiveDriverService.listByIds(TENANT_ID, List.of(1L)))
                .thenReturn(reactor.core.publisher.Flux.just(driver));
        when(driverBuilder.buildVOByBO(driver)).thenReturn(vo);
        StepVerifier.create(withTenant(controller.listByIds(List.of(1L))))
                .assertNext(map -> assertThat(map).containsEntry("1", vo))
                .verifyComplete();
    }
}
