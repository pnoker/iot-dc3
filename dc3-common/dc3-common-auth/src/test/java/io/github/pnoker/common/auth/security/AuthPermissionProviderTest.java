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
package io.github.pnoker.common.auth.security;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.repository.ReactivePermissionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthPermissionProviderTest {

    @Mock
    ReactivePermissionStore store;

    @Test
    void bindingMutationInvalidatesCachedPermissions() {
        when(store.listResourceCodes(7L, 11L)).thenReturn(Flux.just("menu:home"));
        AuthPermissionProvider provider = new AuthPermissionProvider();
        provider.setReactivePermissionStore(store);

        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true)
                .verifyComplete();
        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true)
                .verifyComplete();
        provider.invalidate(7L, 11L);
        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true)
                .verifyComplete();

        verify(store, times(2)).listResourceCodes(7L, 11L);
    }
}
