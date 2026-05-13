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

package io.github.pnoker.common.gateway.filter;

import io.github.pnoker.common.constant.common.RequestConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.UnAuthorizedException;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.gateway.service.FilterService;
import io.github.pnoker.common.utils.HmacAuthSigner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticGatewayFilterTest {

    @Mock
    private FilterService filterService;

    private AuthenticGatewayFilter filter(boolean signingEnabled) {
        return new AuthenticGatewayFilter(filterService,
                new HmacAuthSigner(signingEnabled ? "test-secret" : null));
    }

    private RequestHeader.UserHeader user;
    private FacadeUserLoginBO userLogin;
    private FacadeTenantBO tenant;

    @BeforeEach
    void setUp() {
        user = new RequestHeader.UserHeader(7L, "Alice", "alice", 1L);
        userLogin = new FacadeUserLoginBO();
        userLogin.setLoginName("alice");
        tenant = new FacadeTenantBO();
        tenant.setTenantName("Acme");
    }

    private static MockServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/manager/devices").build());
    }

    private static GatewayFilterChain capturingChain(AtomicReference<ServerWebExchange> capture) {
        return ex -> {
            capture.set(ex);
            return Mono.empty();
        };
    }

    @Test
    void writesXAuthUserAndStripsXAuthSignWhenSigningDisabled() {
        when(filterService.getTenant(any())).thenReturn(tenant);
        when(filterService.getUserLogin(any())).thenReturn(userLogin);
        when(filterService.getUser(userLogin, tenant)).thenReturn(user);

        AtomicReference<ServerWebExchange> capture = new AtomicReference<>();
        MockServerWebExchange initial = MockServerWebExchange.from(MockServerHttpRequest.get("/foo")
                .header(RequestConstant.Header.X_AUTH_SIGN, "smuggled-by-client")
                .build());

        filter(false).filter(initial, capturingChain(capture)).block();

        HttpHeaders forwarded = capture.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(RequestConstant.Header.X_AUTH_USER)).contains("\"userName\":\"alice\"");
        assertThat(forwarded.getFirst(RequestConstant.Header.X_AUTH_SIGN)).isNull();
    }

    @Test
    void writesHmacSignedHeaderWhenSigningEnabled() {
        when(filterService.getTenant(any())).thenReturn(tenant);
        when(filterService.getUserLogin(any())).thenReturn(userLogin);
        when(filterService.getUser(userLogin, tenant)).thenReturn(user);

        AtomicReference<ServerWebExchange> capture = new AtomicReference<>();
        filter(true).filter(exchange(), capturingChain(capture)).block();

        HttpHeaders forwarded = capture.get().getRequest().getHeaders();
        String userJson = forwarded.getFirst(RequestConstant.Header.X_AUTH_USER);
        String sign = forwarded.getFirst(RequestConstant.Header.X_AUTH_SIGN);
        assertThat(userJson).isNotNull();
        assertThat(sign).matches("[0-9a-f]{64}");
        // Signature must verify using the same shared secret.
        assertThat(new HmacAuthSigner("test-secret").verify(userJson, sign)).isTrue();
    }

    @Test
    void responsesAreUnauthorizedWhenFilterServiceThrowsUnauthorized() {
        when(filterService.getTenant(any())).thenReturn(tenant);
        when(filterService.getUserLogin(any())).thenReturn(userLogin);
        doThrow(new UnAuthorizedException("token rejected"))
                .when(filterService).checkValid(any(), any(), any());

        MockServerWebExchange ex = exchange();
        AtomicReference<ServerWebExchange> capture = new AtomicReference<>();
        filter(false).filter(ex, capturingChain(capture)).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(ex.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(capture.get()).isNull();
    }

    @Test
    void responsesAreInternalServerErrorOnUnexpectedException() {
        when(filterService.getTenant(any())).thenThrow(new RuntimeException("boom"));

        MockServerWebExchange ex = exchange();
        AtomicReference<ServerWebExchange> capture = new AtomicReference<>();
        filter(false).filter(ex, capturingChain(capture)).block();

        assertThat(ex.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(capture.get()).isNull();
    }

    @Test
    void filterShortCircuitsAuthLookupOnFailFastValidation() {
        when(filterService.getTenant(any())).thenReturn(null);
        // checkValid is the only place that can throw — when getTenant returns null and
        // checkValid does not throw, the filter still tries to build the user header.
        when(filterService.getUserLogin(any())).thenReturn(null);
        when(filterService.getUser(null, null)).thenReturn(user);

        AtomicReference<ServerWebExchange> capture = new AtomicReference<>();
        filter(false).filter(exchange(), capturingChain(capture)).block();

        verify(filterService).checkValid(any(), any(), any());
        assertThat(capture.get()).isNotNull();
    }
}
