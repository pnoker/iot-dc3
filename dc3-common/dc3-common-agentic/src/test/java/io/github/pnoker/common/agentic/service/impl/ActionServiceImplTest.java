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

package io.github.pnoker.common.agentic.service.impl;

import io.github.pnoker.common.agentic.dal.ActionManager;
import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.entity.builder.ActionBuilder;
import io.github.pnoker.common.agentic.entity.model.ActionDO;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pure-Mockito unit coverage of {@link ActionServiceImpl}. The {@code confirm} and
 * {@code reject} flows are intentionally deferred to integration tests because they
 * build {@code Wrappers.lambdaUpdate()} chains that need the MyBatis-Plus entity cache,
 * which is only populated when a mapper is registered with a real Configuration.
 */
@ExtendWith(MockitoExtension.class)
class ActionServiceImplTest {

    @Mock
    private ActionManager actionManager;

    @Mock
    private ActionBuilder actionBuilder;

    @Mock
    private PointCommandFacade pointCommandFacade;

    private ActionServiceImpl service;
    private RequestHeader.UserHeader header;

    @BeforeEach
    void setUp() {
        service = new ActionServiceImpl(actionManager, actionBuilder, pointCommandFacade);
        header = new RequestHeader.UserHeader();
        header.setTenantId(1L);
        header.setUserId(2L);
        header.setUserName("admin");
    }

    @Test
    void createWritePointValueActionPersistsPendingActionWithExpiry() {
        ActionDO entityDO = new ActionDO();
        entityDO.setActionId("uuid-123");
        when(actionBuilder.buildDOByBO(any(ActionBO.class))).thenReturn(entityDO);

        String actionId = service.createWritePointValueAction("conv", 10L, 20L, "42", header);

        assertThat(actionId).isEqualTo("uuid-123");

        ArgumentCaptor<ActionBO> captor = ArgumentCaptor.forClass(ActionBO.class);
        verify(actionBuilder).buildDOByBO(captor.capture());
        ActionBO captured = captor.getValue();
        assertThat(captured.getActionType()).isEqualTo("writePointValue");
        assertThat(captured.getStatus()).isEqualTo(AgenticActionStatusEnum.PENDING);
        assertThat(captured.getPayload()).containsEntry("deviceId", 10L).containsEntry("pointId", 20L)
                .containsEntry("value", "42");
        assertThat(captured.getExpireTime()).isAfter(LocalDateTime.now());
        assertThat(captured.getTenantId()).isEqualTo(1L);
        assertThat(captured.getUserId()).isEqualTo(2L);
        assertThat(captured.getActionId()).isNotBlank();
        verify(actionManager).save(entityDO);
    }
}
