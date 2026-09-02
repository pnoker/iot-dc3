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
package io.github.pnoker.common.facade.local;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDriverBuilder;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class DriverLocalFacadeTest {
    @Mock
    ReactiveDriverService service;

    @Mock
    FacadeDriverBuilder builder;

    @InjectMocks
    DriverLocalFacade facade;

    @Test
    void listReactiveMapsCanonicalOffsetPage() {
        DriverBO source = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(service.list(any(DriverFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(source), 10, 5, 11)));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listReactive(new FacadeDriverOffsetQuery(
                        7L, null, null, null, null, null, null, null, null, null, 10, 5, List.of())))
                .assertNext(page -> {
                    org.assertj.core.api.Assertions.assertThat(page.offset()).isEqualTo(10);
                    org.assertj.core.api.Assertions.assertThat(page.limit()).isEqualTo(5);
                    org.assertj.core.api.Assertions.assertThat(page.total()).isEqualTo(11);
                    org.assertj.core.api.Assertions.assertThat(page.items()).containsExactly(mapped);
                })
                .verifyComplete();
    }

    @Test
    void listByIdsIsReactiveAndTenantScoped() {
        DriverBO source = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(service.listByIds(7L, List.of(1L))).thenReturn(Flux.just(source));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listByIdsReactive(7L, List.of(1L, 1L)))
                .expectNext(mapped)
                .verifyComplete();
    }
}
