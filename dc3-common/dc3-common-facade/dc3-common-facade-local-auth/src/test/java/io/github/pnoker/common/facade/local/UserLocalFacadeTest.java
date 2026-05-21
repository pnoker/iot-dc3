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

import io.github.pnoker.common.auth.entity.bo.UserBO;
import io.github.pnoker.common.auth.service.UserService;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLocalFacadeTest {

    @Mock
    private UserService userService;

    @Mock
    private FacadeUserBuilder facadeUserBuilder;

    private UserLocalFacade facade;

    private static UserBO any() {
        return org.mockito.ArgumentMatchers.any();
    }

    @BeforeEach
    void setUp() {
        facade = new UserLocalFacade(userService, facadeUserBuilder);
    }

    @Test
    void getByIdReturnsNullWhenServiceReturnsNull() {
        when(userService.getById(1L)).thenReturn(null);
        assertThat(facade.getById(1L)).isNull();
        verify(facadeUserBuilder, never()).toFacadeBO(any());
    }

    @Test
    void getByIdMapsThroughBuilderWhenServiceReturnsValue() {
        UserBO user = new UserBO();
        FacadeUserBO mapped = new FacadeUserBO();
        when(userService.getById(1L)).thenReturn(user);
        when(facadeUserBuilder.toFacadeBO(user)).thenReturn(mapped);

        assertThat(facade.getById(1L)).isSameAs(mapped);
    }

}
