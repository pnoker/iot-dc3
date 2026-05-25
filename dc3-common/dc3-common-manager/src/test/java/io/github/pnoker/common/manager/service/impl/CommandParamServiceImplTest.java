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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.dal.CommandParamManager;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.builder.CommandParamBuilder;
import io.github.pnoker.common.manager.entity.model.CommandParamDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DriverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandParamServiceImplTest {

    @Mock
    private CommandParamBuilder commandParamBuilder;

    @Mock
    private CommandParamManager commandParamManager;

    @Mock
    private CommandService commandService;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private CommandParamServiceImpl service;

    private CommandParamBO bo;
    private CommandParamDO doRow;
    private CommandBO command;

    @BeforeEach
    void setUp() {
        bo = new CommandParamBO();
        bo.setId(1L);
        bo.setParamName("speed");
        bo.setParamCode("speed");
        bo.setCommandId(10L);
        bo.setTenantId(100L);

        doRow = new CommandParamDO();
        doRow.setId(1L);
        doRow.setParamName("speed");
        doRow.setParamCode("speed");
        doRow.setCommandId(10L);
        doRow.setTenantId(100L);

        command = new CommandBO();
        command.setId(10L);
        command.setProfileId(20L);
        command.setTenantId(100L);
    }

    @Test
    void addPublishesOwningCommandUpdate() {
        when(commandService.getById(10L)).thenReturn(command);
        when(commandParamManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(commandParamBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(commandParamManager.save(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.add(bo);

        assertMetadataEvent(MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    @Test
    void updatePublishesOwningCommandUpdate() {
        when(commandParamManager.getById(1L)).thenReturn(doRow);
        when(commandService.getById(10L)).thenReturn(command);
        when(commandParamManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(commandParamBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(commandParamManager.updateById(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.update(bo);

        assertMetadataEvent(MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    @Test
    void deletePublishesOwningCommandUpdate() {
        when(commandParamManager.getById(1L)).thenReturn(doRow);
        when(commandParamManager.removeById(1L)).thenReturn(true);
        when(commandService.getById(10L)).thenReturn(command);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        service.delete(1L);

        assertMetadataEvent(MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE, 10L);
    }

    private void assertMetadataEvent(MetadataTypeEnum metadataType, MetadataOperateTypeEnum operateType, Long id) {
        ArgumentCaptor<MetadataEvent> captor = ArgumentCaptor.forClass(MetadataEvent.class);
        verify(metadataEventPublisher).publishEvent(captor.capture());

        MetadataEvent event = captor.getValue();
        assertThat(event.getMetadataType()).isEqualTo(metadataType);
        assertThat(event.getOperateType()).isEqualTo(operateType);
        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getTargetServices()).isEmpty();
    }

}
