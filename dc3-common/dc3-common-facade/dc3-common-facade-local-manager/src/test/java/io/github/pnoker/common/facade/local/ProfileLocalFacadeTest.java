package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.common.facade.local.builder.FacadeProfileBuilder;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.manager.service.ReactiveProfileService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileLocalFacadeTest {
    @Mock ReactiveProfileService service;
    @Mock FacadeProfileBuilder builder;
    @InjectMocks ProfileLocalFacade facade;

    @Test
    void listReactiveMapsCanonicalOffsetPage() {
        ProfileBO source = new ProfileBO();
        FacadeProfileBO mapped = new FacadeProfileBO();
        when(service.list(any(ProfileFilter.class))).thenReturn(Mono.just(OffsetPage.of(List.of(source), 5, 5, 6)));
        when(builder.toFacadeBO(source)).thenReturn(mapped);
        StepVerifier.create(facade.listReactive(new FacadeProfileOffsetQuery(7L, null, null, null, null, null, null, null, null, null, 5, 5, List.of())))
                .assertNext(page -> {
                    org.assertj.core.api.Assertions.assertThat(page.offset()).isEqualTo(5);
                    org.assertj.core.api.Assertions.assertThat(page.items()).containsExactly(mapped);
                }).verifyComplete();
    }
}
