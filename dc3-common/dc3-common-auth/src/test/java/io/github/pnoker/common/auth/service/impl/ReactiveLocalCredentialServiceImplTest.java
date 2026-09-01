package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.entity.builder.LocalCredentialBuilder;
import io.github.pnoker.common.auth.entity.model.LocalCredentialDO;
import io.github.pnoker.common.auth.repository.ReactiveLocalCredentialStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveLocalCredentialServiceImplTest {

    @Mock ReactiveLocalCredentialStore store;
    @Mock LocalCredentialBuilder builder;

    @Test
    void rejectsBlankLoginNameWithoutTouchingStore() {
        ReactiveLocalCredentialServiceImpl service = new ReactiveLocalCredentialServiceImpl(store, builder);

        StepVerifier.create(service.getByLoginName(1L, " "))
                .expectError().verify();

        verifyNoInteractions(store);
    }

    @Test
    void mapsTenantScopedCredential() {
        LocalCredentialDO row = new LocalCredentialDO();
        LocalCredentialBO value = new LocalCredentialBO();
        when(store.getById(7L, 11L)).thenReturn(Mono.just(row));
        when(builder.buildBOByDO(row)).thenReturn(value);

        StepVerifier.create(new ReactiveLocalCredentialServiceImpl(store, builder).getById(7L, 11L))
                .expectNext(value).verifyComplete();

        verify(store).getById(7L, 11L);
        verify(builder).buildBOByDO(row);
    }

    @Test
    void availabilityChecksDisabledCredentialsToo() {
        when(store.existsByLoginName(7L, "alice")).thenReturn(Mono.just(true));

        StepVerifier.create(new ReactiveLocalCredentialServiceImpl(store, builder)
                        .isLoginNameAvailable(7L, " Alice "))
                .expectNext(false).verifyComplete();

        verify(store).existsByLoginName(7L, "alice");
    }

    @Test
    void passwordVerificationRunsAndRejectsLockedCredential() {
        LocalCredentialBO credential = new LocalCredentialBO();
        credential.setLockedUntil(LocalDateTime.now().plusMinutes(5));

        StepVerifier.create(new ReactiveLocalCredentialServiceImpl(store, builder)
                        .verifyPassword(credential, "password"))
                .expectNext(false).verifyComplete();

        verifyNoInteractions(store);
    }
}
