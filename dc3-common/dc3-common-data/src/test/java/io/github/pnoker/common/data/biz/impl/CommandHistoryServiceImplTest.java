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

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.data.dal.CommandHistoryManager;
import io.github.pnoker.common.data.entity.builder.CommandHistoryBuilder;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.mapper.EntityStateMapper;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandHistoryServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private CommandFacade commandFacade;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private CommandHistoryManager commandHistoryManager;

    @Mock
    private EntityStateMapper entityStateMapper;

    @Mock
    private CommandHistoryBuilder commandHistoryBuilder;

    private CommandHistoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CommandHistoryServiceImpl(deviceFacade, driverFacade, commandFacade, rabbitTemplate,
                commandHistoryManager, commandHistoryBuilder, entityStateMapper);
    }

    @Test
    void callUsesCommandTimeoutSecondsForRecordAndDispatchExpiry() {
        Long tenantId = 100L;
        Long deviceId = 10L;
        Long commandId = 20L;

        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setProfileId(30L);
        device.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeCommandBO command = new FacadeCommandBO();
        command.setId(commandId);
        command.setTenantId(tenantId);
        command.setProfileId(30L);
        command.setCommandCode("restart");
        command.setTimeout(5);
        command.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeDriverBO driver = new FacadeDriverBO();
        driver.setId(40L);
        driver.setServiceName("modbus-driver");

        EntityStateDO driverState = new EntityStateDO();
        driverState.setStateFlag(EntityStatusEnum.ONLINE.getIndex());

        CommandCallVO call = new CommandCallVO();
        call.setDeviceId(deviceId);
        call.setCommandId(commandId);
        call.setParamValues(Map.of("mode", "soft"));

        when(deviceFacade.getById(tenantId, deviceId)).thenReturn(device);
        when(commandFacade.getById(tenantId, commandId)).thenReturn(command);
        when(driverFacade.getByDeviceId(tenantId, deviceId)).thenReturn(driver);
        when(entityStateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(driverState);

        LocalDateTime beforeLocal = LocalDateTime.now();
        Instant beforeInstant = Instant.now();
        String recordId = service.call(tenantId, call);
        LocalDateTime afterLocal = LocalDateTime.now();
        Instant afterInstant = Instant.now();

        ArgumentCaptor<CommandHistoryDO> recordCaptor = ArgumentCaptor.forClass(CommandHistoryDO.class);
        verify(commandHistoryManager).save(recordCaptor.capture());
        CommandHistoryDO savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getRecordId()).isEqualTo(recordId);
        assertThat(savedRecord.getStatus()).isEqualTo(PointCommandStatusEnum.SENT);
        assertThat(savedRecord.getExpireTime()).isAfterOrEqualTo(beforeLocal.plusSeconds(5));
        assertThat(savedRecord.getExpireTime()).isBeforeOrEqualTo(afterLocal.plusSeconds(5));

        ArgumentCaptor<CommandCallDTO> dtoCaptor = ArgumentCaptor.forClass(CommandCallDTO.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConstant.TOPIC_EXCHANGE_COMMAND),
                eq(RabbitConstant.ROUTING_COMMAND_PREFIX + "modbus-driver"), dtoCaptor.capture(),
                any(CorrelationData.class));
        CommandCallDTO dto = dtoCaptor.getValue();
        assertThat(dto.recordId()).isEqualTo(recordId);
        assertThat(dto.expireAt()).isAfterOrEqualTo(beforeInstant.plusSeconds(5));
        assertThat(dto.expireAt()).isBeforeOrEqualTo(afterInstant.plusSeconds(5));
        verify(commandFacade).getById(tenantId, commandId);
    }

    @Test
    void callTreatsLegacyMillisecondTimeoutAsSeconds() {
        Long tenantId = 100L;
        Long deviceId = 10L;
        Long commandId = 20L;

        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setProfileId(30L);
        device.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeCommandBO command = new FacadeCommandBO();
        command.setId(commandId);
        command.setTenantId(tenantId);
        command.setProfileId(30L);
        command.setCommandCode("restart");
        command.setTimeout(30000);
        command.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeDriverBO driver = new FacadeDriverBO();
        driver.setId(40L);
        driver.setServiceName("modbus-driver");

        EntityStateDO driverState = new EntityStateDO();
        driverState.setStateFlag(EntityStatusEnum.ONLINE.getIndex());

        CommandCallVO call = new CommandCallVO();
        call.setDeviceId(deviceId);
        call.setCommandId(commandId);

        when(deviceFacade.getById(tenantId, deviceId)).thenReturn(device);
        when(commandFacade.getById(tenantId, commandId)).thenReturn(command);
        when(driverFacade.getByDeviceId(tenantId, deviceId)).thenReturn(driver);
        when(entityStateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(driverState);

        LocalDateTime beforeLocal = LocalDateTime.now();
        Instant beforeInstant = Instant.now();
        service.call(tenantId, call);
        LocalDateTime afterLocal = LocalDateTime.now();
        Instant afterInstant = Instant.now();

        ArgumentCaptor<CommandHistoryDO> recordCaptor = ArgumentCaptor.forClass(CommandHistoryDO.class);
        verify(commandHistoryManager).save(recordCaptor.capture());
        CommandHistoryDO savedRecord = recordCaptor.getValue();
        assertThat(savedRecord.getExpireTime()).isAfterOrEqualTo(beforeLocal.plusSeconds(30));
        assertThat(savedRecord.getExpireTime()).isBeforeOrEqualTo(afterLocal.plusSeconds(30));

        ArgumentCaptor<CommandCallDTO> dtoCaptor = ArgumentCaptor.forClass(CommandCallDTO.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConstant.TOPIC_EXCHANGE_COMMAND),
                eq(RabbitConstant.ROUTING_COMMAND_PREFIX + "modbus-driver"), dtoCaptor.capture(),
                any(CorrelationData.class));
        CommandCallDTO dto = dtoCaptor.getValue();
        assertThat(dto.expireAt()).isAfterOrEqualTo(beforeInstant.plusSeconds(30));
        assertThat(dto.expireAt()).isBeforeOrEqualTo(afterInstant.plusSeconds(30));
        verify(commandFacade).getById(tenantId, commandId);
    }

    @Test
    void callResolvesCommandByCodeWithinDeviceProfile() {
        Long tenantId = 100L;
        Long deviceId = 10L;
        Long commandId = 20L;

        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        device.setProfileId(30L);
        device.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeCommandBO command = new FacadeCommandBO();
        command.setId(commandId);
        command.setTenantId(tenantId);
        command.setProfileId(30L);
        command.setCommandCode("restart");
        command.setTimeout(5);
        command.setEnableFlag(EnableFlagEnum.ENABLE);

        FacadeDriverBO driver = new FacadeDriverBO();
        driver.setId(40L);
        driver.setServiceName("modbus-driver");

        EntityStateDO driverState = new EntityStateDO();
        driverState.setStateFlag(EntityStatusEnum.ONLINE.getIndex());

        CommandCallVO call = new CommandCallVO();
        call.setDeviceId(deviceId);
        call.setCommandCode("restart");

        when(deviceFacade.getById(tenantId, deviceId)).thenReturn(device);
        when(commandFacade.listByPage(any(FacadeCommandQuery.class)))
                .thenReturn(new FacadePage<>(1L, 1L, 1L, 1L, List.of(command)));
        when(driverFacade.getByDeviceId(tenantId, deviceId)).thenReturn(driver);
        when(entityStateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(driverState);

        service.call(tenantId, call);

        ArgumentCaptor<FacadeCommandQuery> queryCaptor = ArgumentCaptor.forClass(FacadeCommandQuery.class);
        verify(commandFacade).listByPage(queryCaptor.capture());
        assertThat(queryCaptor.getValue().getTenantId()).isEqualTo(tenantId);
        assertThat(queryCaptor.getValue().getProfileId()).isEqualTo(30L);
        assertThat(queryCaptor.getValue().getCommandCode()).isEqualTo("restart");

        ArgumentCaptor<CommandCallDTO> dtoCaptor = ArgumentCaptor.forClass(CommandCallDTO.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitConstant.TOPIC_EXCHANGE_COMMAND),
                eq(RabbitConstant.ROUTING_COMMAND_PREFIX + "modbus-driver"), dtoCaptor.capture(),
                any(CorrelationData.class));
        assertThat(dtoCaptor.getValue().commandId()).isEqualTo(commandId);
    }

}
