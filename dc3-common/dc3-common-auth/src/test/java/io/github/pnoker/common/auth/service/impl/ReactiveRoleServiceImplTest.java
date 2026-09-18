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
package io.github.pnoker.common.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.auth.entity.bo.RoleBO;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.model.RoleDO;
import io.github.pnoker.common.auth.repository.ReactiveRoleStore;
import io.github.pnoker.common.auth.repository.RoleFilter;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ReactiveRoleServiceImplTest {
    @Mock
    ReactiveRoleStore store;

    @Mock
    RoleBuilder builder;

    @Test
    void listMapsOffsetPage() {
        RoleDO row = new RoleDO();
        RoleBO bo = new RoleBO();
        when(store.list(any(RoleFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(row), 10, 5, 11)));
        when(builder.buildBOByDO(row)).thenReturn(bo);
        StepVerifier.create(service()
                        .list(new RoleFilter(
                                7L,
                                null,
                                null,
                                null,
                                new io.github.pnoker.db.r2dbc.core.page.PageRequest(10, 5, List.of()))))
                .assertNext(p -> {
                    assertThat(p.items()).containsExactly(bo);
                    assertThat(p.offset()).isEqualTo(10);
                    assertThat(p.total()).isEqualTo(11);
                })
                .verifyComplete();
    }

    @Test
    void getMissingRoleFailsClosed() {
        when(store.getById(7L, 9L)).thenReturn(Mono.empty());
        StepVerifier.create(service().getById(7L, 9L))
                .expectErrorMessage("Role")
                .verify();
    }

    private ReactiveRoleServiceImpl service() {
        return new ReactiveRoleServiceImpl(store, builder);
    }
}
