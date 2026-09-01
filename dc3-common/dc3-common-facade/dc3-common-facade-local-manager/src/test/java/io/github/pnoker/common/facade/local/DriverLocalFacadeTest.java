package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeDriverBuilder;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.repository.DriverFilter;
import io.github.pnoker.common.manager.service.ReactiveDriverService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverLocalFacadeTest {
    @Mock ReactiveDriverService service;
    @Mock FacadeDriverBuilder builder;
    @InjectMocks DriverLocalFacade facade;

    @Test
    void listReactiveMapsCanonicalOffsetPage() {
        DriverBO source = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(service.list(any(DriverFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(source), 10, 5, 11)));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listReactive(new FacadeDriverOffsetQuery(7L, null, null, null, null, null, null, null, null, null, 10, 5, List.of())))
                .assertNext(page -> {
                    org.assertj.core.api.Assertions.assertThat(page.offset()).isEqualTo(10);
                    org.assertj.core.api.Assertions.assertThat(page.limit()).isEqualTo(5);
                    org.assertj.core.api.Assertions.assertThat(page.total()).isEqualTo(11);
                    org.assertj.core.api.Assertions.assertThat(page.items()).containsExactly(mapped);
                }).verifyComplete();
    }

    @Test
    void listByIdsIsReactiveAndTenantScoped() {
        DriverBO source = new DriverBO();
        FacadeDriverBO mapped = new FacadeDriverBO();
        when(service.listByIds(7L, List.of(1L))).thenReturn(Flux.just(source));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listByIdsReactive(7L, List.of(1L, 1L))).expectNext(mapped).verifyComplete();
    }
}
