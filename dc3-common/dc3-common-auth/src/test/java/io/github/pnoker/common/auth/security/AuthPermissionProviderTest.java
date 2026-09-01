package io.github.pnoker.common.auth.security;

import io.github.pnoker.common.auth.repository.ReactivePermissionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthPermissionProviderTest {

    @Mock ReactivePermissionStore store;

    @Test
    void bindingMutationInvalidatesCachedPermissions() {
        when(store.listResourceCodes(7L, 11L)).thenReturn(Flux.just("menu:home"));
        AuthPermissionProvider provider = new AuthPermissionProvider();
        provider.setReactivePermissionStore(store);

        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true).verifyComplete();
        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true).verifyComplete();
        provider.invalidate(7L, 11L);
        StepVerifier.create(provider.hasPermission(7L, 11L, "menu:home"))
                .expectNext(true).verifyComplete();

        verify(store, times(2)).listResourceCodes(7L, 11L);
    }
}
