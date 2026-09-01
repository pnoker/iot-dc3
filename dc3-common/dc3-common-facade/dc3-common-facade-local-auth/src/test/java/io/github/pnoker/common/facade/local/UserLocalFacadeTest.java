package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.service.ReactiveUserService;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLocalFacadeTest {

    @Mock
    private ReactiveUserService userService;

    @Mock
    private FacadeUserBuilder userBuilder;

    private UserLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new UserLocalFacade(userService, userBuilder);
    }

    @Test
    void getByIdPreservesTenantScopeAndMapsUser() {
        UserBO user = new UserBO();
        FacadeUserBO mapped = new FacadeUserBO();
        when(userService.getById(11L, 7L)).thenReturn(Mono.just(user));
        when(userBuilder.toFacadeBO(user)).thenReturn(mapped);

        StepVerifier.create(facade.getById(11L, 7L)).expectNext(mapped).verifyComplete();
    }

    @Test
    void getByPrincipalIdConvertsNotFoundToEmpty() {
        when(userService.getByPrincipalId(11L, 100L)).thenReturn(Mono.error(new NotFoundException("missing")));

        StepVerifier.create(facade.getByPrincipalId(11L, 100L)).verifyComplete();
    }
}
