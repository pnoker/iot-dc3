package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.auth.biz.ReactiveTokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenLocalFacadeTest {

    @Mock
    private ReactiveTokenService tokenService;

    private TokenLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new TokenLocalFacade(tokenService);
    }

    @Test
    void checkValidMapsValidToken() {
        TokenValid token = new TokenValid();
        token.setValid(true);
        when(tokenService.checkValid("alice", "token", "tenant-a")).thenReturn(Mono.just(token));

        StepVerifier.create(facade.checkValid("tenant-a", "alice", "token")).expectNext(true).verifyComplete();
    }

    @Test
    void checkValidMapsInvalidToken() {
        TokenValid token = new TokenValid();
        token.setValid(false);
        when(tokenService.checkValid("alice", "token", "tenant-a")).thenReturn(Mono.just(token));

        StepVerifier.create(facade.checkValid("tenant-a", "alice", "token")).expectNext(false).verifyComplete();
    }
}
