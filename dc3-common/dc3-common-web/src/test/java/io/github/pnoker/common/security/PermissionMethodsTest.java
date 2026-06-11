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

package io.github.pnoker.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.test.StepVerifier;

class PermissionMethodsTest {

    private final PermissionMethods permissionMethods = new PermissionMethods("dc3-center-auth");

    @Test
    void canUsesWildcardFromReactiveSecurityContext() {
        TestingAuthenticationToken auth = authenticated(PermissionMethods.WILDCARD);

        StepVerifier.create(permissionMethods.can("menu", "list")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void canMatchesServiceScopedPermission() {
        TestingAuthenticationToken auth = authenticated("dc3-center-auth:menu:list");

        StepVerifier.create(permissionMethods.can("menu", "list")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void canRejectsMissingPermission() {
        TestingAuthenticationToken auth = authenticated("dc3-center-auth:menu:get");

        StepVerifier.create(permissionMethods.can("menu", "list")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void canRejectsAnonymousContext() {
        StepVerifier.create(permissionMethods.can("menu", "list"))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void anyMatchesOneServiceScopedPermission() {
        TestingAuthenticationToken auth = authenticated("dc3-center-auth:menu:list");

        StepVerifier.create(permissionMethods.any("menu:get", "menu:list")
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth)))
                .expectNext(true)
                .verifyComplete();
    }

    private TestingAuthenticationToken authenticated(String authority) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("dc3", "n/a", authority);
        auth.setAuthenticated(true);
        return auth;
    }
}
