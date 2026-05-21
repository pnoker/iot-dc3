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

import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.service.UserLoginService;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.facade.local.builder.FacadeUserLoginBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginLocalFacadeTest {

    @Mock
    private UserLoginService userLoginService;

    @Mock
    private FacadeUserLoginBuilder facadeUserLoginBuilder;

    private UserLoginLocalFacade facade;

    private static UserLoginBO any() {
        return org.mockito.ArgumentMatchers.any();
    }

    @BeforeEach
    void setUp() {
        facade = new UserLoginLocalFacade(userLoginService, facadeUserLoginBuilder);
    }

    @Test
    void getByNameReturnsNullWhenServiceReturnsNull() {
        when(userLoginService.getByLoginName(eq("alice"), eq(false))).thenReturn(null);
        assertThat(facade.getByName("alice")).isNull();
        verify(facadeUserLoginBuilder, never()).toFacadeBO(any());
    }

    @Test
    void getByNameDelegatesToServiceWithExplicitFalseFlag() {
        UserLoginBO bo = new UserLoginBO();
        FacadeUserLoginBO mapped = new FacadeUserLoginBO();
        when(userLoginService.getByLoginName(eq("alice"), eq(false))).thenReturn(bo);
        when(facadeUserLoginBuilder.toFacadeBO(bo)).thenReturn(mapped);

        assertThat(facade.getByName("alice")).isSameAs(mapped);
        // Boolean argument is the soft-delete-include flag; local facade pins it to false to
        // match grpc UserLoginServer's contract.
        verify(userLoginService).getByLoginName("alice", false);
    }

}
