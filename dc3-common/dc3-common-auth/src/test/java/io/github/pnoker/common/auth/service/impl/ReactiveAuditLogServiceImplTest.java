package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.IdentityAuditLogBO;
import io.github.pnoker.common.auth.entity.builder.IdentityAuditLogBuilder;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogQueryStore;
import io.github.pnoker.common.auth.repository.ReactiveAuditLogStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveAuditLogServiceImplTest {

    @Mock
    private ReactiveAuditLogStore store;
    @Mock
    private ReactiveAuditLogQueryStore queryStore;
    @Mock
    private IdentityAuditLogBuilder builder;

    @Test
    void appendsTenantScopedEvent() {
        when(store.append(any(IdentityAuditLogBO.class))).thenReturn(Mono.empty());

        StepVerifier.create(service().log(7L, 9L, "USER",
                        "CREATE", "tenant_membership", 11L, "", "SUCCESS", null))
                .verifyComplete();

        ArgumentCaptor<IdentityAuditLogBO> captor = ArgumentCaptor.forClass(IdentityAuditLogBO.class);
        verify(store).append(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getPrincipalId()).isEqualTo(9L);
        assertThat(captor.getValue().getAction()).isEqualTo("CREATE");
    }

    @Test
    void auditFailureDoesNotFailBusinessPublisher() {
        when(store.append(any(IdentityAuditLogBO.class))).thenReturn(Mono.error(new IllegalStateException("db down")));

        StepVerifier.create(service().log(7L, 9L, "USER",
                        "DELETE", "tenant_membership", 11L, null, "SUCCESS", null))
                .verifyComplete();
    }

    @Test
    void invalidTenantIsRejectedBeforeStoreAccess() {
        StepVerifier.create(service().log(0L, 9L, "USER",
                        "CREATE", "tenant_membership", 11L, null, "SUCCESS", null))
                .verifyComplete();

        verifyNoInteractions(store);
    }

    private ReactiveAuditLogServiceImpl service() {
        return new ReactiveAuditLogServiceImpl(store, queryStore, builder);
    }
}
