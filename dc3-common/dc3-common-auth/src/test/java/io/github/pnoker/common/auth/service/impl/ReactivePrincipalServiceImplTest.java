package io.github.pnoker.common.auth.service.impl;

import io.github.pnoker.common.auth.entity.bo.PrincipalBO;
import io.github.pnoker.common.auth.entity.builder.PrincipalBuilder;
import io.github.pnoker.common.auth.entity.model.PrincipalDO;
import io.github.pnoker.common.auth.repository.PrincipalFilter;
import io.github.pnoker.common.auth.repository.ReactivePrincipalStore;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivePrincipalServiceImplTest {

    @Mock
    private ReactivePrincipalStore principalStore;

    @Mock
    private PrincipalBuilder principalBuilder;

    private ReactivePrincipalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReactivePrincipalServiceImpl(principalStore, principalBuilder);
    }

    @Test
    void getByIdRejectsMissingPrincipal() {
        when(principalStore.getById(1L, 42L)).thenReturn(Mono.empty());

        StepVerifier.create(service.getById(1L, 42L)).expectError(NotFoundException.class).verify();
    }

    @Test
    void listMapsOffsetPageWithoutLegacyPage() {
        PrincipalDO row = new PrincipalDO();
        PrincipalBO mapped = new PrincipalBO();
        when(principalStore.list(any(Long.class), any(PrincipalFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(row), 10, 5, 20)));
        when(principalBuilder.buildBOByDO(row)).thenReturn(mapped);

        StepVerifier.create(service.list(1L, new PrincipalFilter(null, null, null, null, EnableFlagEnum.ENABLE, 10, 5, List.of())))
                .assertNext(page -> {
                    assertThat(page.items()).containsExactly(mapped);
                    assertThat(page.offset()).isEqualTo(10);
                    assertThat(page.total()).isEqualTo(20);
                    assertThat(page.hasNext()).isTrue();
                }).verifyComplete();
    }

    @Test
    void listByIdsReturnsReactiveFlux() {
        PrincipalDO row = new PrincipalDO();
        PrincipalBO mapped = new PrincipalBO();
        when(principalStore.listByIds(1L, List.of(7L))).thenReturn(Flux.just(row));
        when(principalBuilder.buildBOByDO(row)).thenReturn(mapped);

        StepVerifier.create(service.listByIds(1L, List.of(7L))).expectNext(mapped).verifyComplete();
    }

    @Test
    void setEnableFlagMapsUpdatedPrincipal() {
        PrincipalDO row = new PrincipalDO();
        PrincipalBO mapped = new PrincipalBO();
        when(principalStore.updateEnableFlag(1L, 7L, EnableFlagEnum.DISABLE.getIndex(), 1L, "admin"))
                .thenReturn(Mono.just(row));
        when(principalBuilder.buildBOByDO(row)).thenReturn(mapped);

        StepVerifier.create(service.setEnableFlag(1L, 7L, EnableFlagEnum.DISABLE, 1L, "admin"))
                .expectNext(mapped).verifyComplete();
        verify(principalStore, never()).touchLastLogin(any());
    }
}
