/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 */
package io.github.pnoker.common.manager.service.impl;

import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveProfileServiceImplTest {
    @Mock private ReactiveProfileStore profileStore;

    @Test
    void listDelegatesTenantBoundFilter() {
        ReactiveProfileServiceImpl service = new ReactiveProfileServiceImpl(profileStore);
        ProfileFilter filter = new ProfileFilter(7L, "sensor", null, null, null, null,
                null, null, null, null, 0, 20, List.of());
        OffsetPage<ProfileBO> page = OffsetPage.of(List.of(), 0, 20, 0);
        when(profileStore.list(filter)).thenReturn(Mono.just(page));

        StepVerifier.create(service.list(filter)).expectNext(page).verifyComplete();
        verify(profileStore).list(filter);
    }

    @Test
    void rejectsMissingLookupIdentity() {
        ReactiveProfileServiceImpl service = new ReactiveProfileServiceImpl(profileStore);
        StepVerifier.create(service.getById(null, 1L))
                .expectErrorMessage("Tenant ID and profile ID are required").verify();
    }
    @Test void rejectsInvalidAddWithoutDatabaseAccess() {
        ProfileBO profile = new ProfileBO(); profile.setTenantId(7L);
        StepVerifier.create(new ReactiveProfileServiceImpl(profileStore).add(profile)).expectErrorMessage("Tenant ID and profile name are required").verify();
        org.mockito.Mockito.verifyNoInteractions(profileStore);
    }
}
