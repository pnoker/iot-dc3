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
import io.github.pnoker.common.manager.dal.CommandManager;
import io.github.pnoker.common.manager.dal.CommandParamManager;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.CommandBuilder;
import io.github.pnoker.common.manager.entity.model.CommandDO;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.CommandMapper;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.ProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceImplTest {

    @Mock
    private CommandBuilder commandBuilder;

    @Mock
    private CommandManager commandManager;

    @Mock
    private CommandMapper commandMapper;

    @Mock
    private CommandParamManager commandParamManager;

    @Mock
    private MetadataEventPublisher metadataEventPublisher;

    @Mock
    private ProfileService profileService;

    @Mock
    private DeviceMapper deviceMapper;

    @Mock
    private DriverService driverService;

    @InjectMocks
    private CommandServiceImpl service;

    private CommandBO bo;
    private CommandDO doRow;
    private ProfileBO profile;

    @BeforeEach
    void setUp() {
        bo = new CommandBO();
        bo.setId(1L);
        bo.setCommandName("Restart");
        bo.setCommandCode("restart");
        bo.setProfileId(5L);
        bo.setTenantId(100L);

        doRow = new CommandDO();
        doRow.setId(1L);
        doRow.setCommandName("Restart");
        doRow.setCommandCode("restart");
        doRow.setProfileId(5L);
        doRow.setTenantId(100L);

        profile = new ProfileBO();
        profile.setId(5L);
        profile.setTenantId(100L);
    }

    @Test
    void saveIgnoresClientCommandCode() {
        bo.setCommandCode("client-code");
        doRow.setCommandCode("generated-code");
        when(profileService.getById(5L)).thenReturn(profile);
        when(commandManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(commandBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(commandManager.save(doRow)).thenReturn(true);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertThatNoException().isThrownBy(() -> service.add(bo));
        assertThat(bo.getCommandCode()).isNull();
    }

    @Test
    void updateKeepsGeneratedCommandCodeImmutable() {
        bo.setCommandCode("client-change");
        when(commandManager.getById(1L)).thenReturn(doRow);
        when(profileService.getById(5L)).thenReturn(profile);
        when(deviceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        when(commandManager.getOne(any(LambdaQueryWrapper.class))).thenReturn(doRow);
        when(commandBuilder.buildDOByBO(bo)).thenReturn(doRow);
        when(commandManager.updateById(doRow)).thenReturn(true);

        assertThatNoException().isThrownBy(() -> service.update(bo));
        assertThat(bo.getCommandCode()).isEqualTo("restart");
    }

}
