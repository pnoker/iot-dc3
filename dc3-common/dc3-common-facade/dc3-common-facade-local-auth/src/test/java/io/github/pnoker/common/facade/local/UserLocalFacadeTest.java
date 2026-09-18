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
