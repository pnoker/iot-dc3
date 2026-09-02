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
package io.github.pnoker.common.manager.service.impl;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.repository.ReactiveProfileStore;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveProfileServiceImplTest {
    @Mock
    private ReactiveProfileStore profileStore;

    @Test
    void listDelegatesTenantBoundFilter() {
        ReactiveProfileServiceImpl service = new ReactiveProfileServiceImpl(profileStore);
        ProfileFilter filter =
                new ProfileFilter(7L, "sensor", null, null, null, null, null, null, null, null, 0, 20, List.of());
        OffsetPage<ProfileBO> page = OffsetPage.of(List.of(), 0, 20, 0);
        when(profileStore.list(filter)).thenReturn(Mono.just(page));

        StepVerifier.create(service.list(filter)).expectNext(page).verifyComplete();
        verify(profileStore).list(filter);
    }

    @Test
    void rejectsMissingLookupIdentity() {
        ReactiveProfileServiceImpl service = new ReactiveProfileServiceImpl(profileStore);
        StepVerifier.create(service.getById(null, 1L))
                .expectErrorMessage("Tenant ID and profile ID are required")
                .verify();
    }

    @Test
    void rejectsInvalidAddWithoutDatabaseAccess() {
        ProfileBO profile = new ProfileBO();
        profile.setTenantId(7L);
        StepVerifier.create(new ReactiveProfileServiceImpl(profileStore).add(profile))
                .expectErrorMessage("Tenant ID and profile name are required")
                .verify();
        org.mockito.Mockito.verifyNoInteractions(profileStore);
    }
}
