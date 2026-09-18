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

import static org.mockito.Mockito.when;

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

        StepVerifier.create(facade.getByLoginName(11L, "alice"))
                .expectNext(mapped)
                .verifyComplete();
    }

    @Test
    void getByLoginNameCompletesEmptyWhenMissing() {
        when(credentialService.getByLoginName(11L, "alice")).thenReturn(Mono.empty());

        StepVerifier.create(facade.getByLoginName(11L, "alice")).verifyComplete();
    }
}
