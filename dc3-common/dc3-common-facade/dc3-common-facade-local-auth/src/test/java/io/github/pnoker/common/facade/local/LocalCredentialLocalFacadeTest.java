package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.entity.bo.LocalCredentialBO;
import io.github.pnoker.common.auth.service.ReactiveLocalCredentialService;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.facade.local.builder.FacadeLocalCredentialBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalCredentialLocalFacadeTest {

    @Mock
    private ReactiveLocalCredentialService credentialService;

    @Mock
    private FacadeLocalCredentialBuilder credentialBuilder;

    private LocalCredentialLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new LocalCredentialLocalFacade(credentialService, credentialBuilder);
    }

    @Test
    void getByLoginNamePreservesTenantScopeAndMapsCredential() {
        LocalCredentialBO credential = new LocalCredentialBO();
        FacadeLocalCredentialBO mapped = new FacadeLocalCredentialBO();
        when(credentialService.getByLoginName(11L, "alice")).thenReturn(Mono.just(credential));
        when(credentialBuilder.toFacadeBO(credential)).thenReturn(mapped);

        StepVerifier.create(facade.getByLoginName(11L, "alice")).expectNext(mapped).verifyComplete();
    }

    @Test
    void getByLoginNameCompletesEmptyWhenMissing() {
        when(credentialService.getByLoginName(11L, "alice")).thenReturn(Mono.empty());

        StepVerifier.create(facade.getByLoginName(11L, "alice")).verifyComplete();
    }
}
