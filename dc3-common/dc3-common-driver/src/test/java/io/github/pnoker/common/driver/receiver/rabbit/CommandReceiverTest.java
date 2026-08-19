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

package io.github.pnoker.common.driver.receiver.rabbit;

import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.facade.api.CommandFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandReceiverTest {

    @Mock
    private DriverCustomService driverCustomService;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private CommandFacade commandFacade;

    @Mock
    private DeviceMetadata deviceMetadata;

    @Mock
    private CommandDedupCache dedupCache;

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private Acknowledgment ack;

    private CommandReceiver receiver;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.setNode("node-a");
        receiver = new CommandReceiver(driverCustomService, driverSenderService, commandFacade,
                deviceMetadata, dedupCache, new DeviceLockManager(), driverMetadata, properties);
        lenient().when(driverMetadata.getFencingToken(10L)).thenReturn(77L);

    }

    @Test
    void commandIsExecutedAndResultIsSent() throws Exception {
        when(dedupCache.tryAcquire("record-1")).thenReturn(true);
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        FacadeCommandBO command = new FacadeCommandBO();
        command.setId(20L);
        command.setTenantId(100L);
        when(commandFacade.getById(100L, 20L)).thenReturn(command);
        Map<String, AttributeBO> driverConfig = Map.of("host", AttributeBO.builder().value("127.0.0.1").build());
        Map<String, AttributeBO> commandConfig = Map.of("address", AttributeBO.builder().value("A1").build());
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getCommandConfig(10L, 20L)).thenReturn(commandConfig);
        when(driverCustomService.execute(eq(driverConfig), eq(commandConfig), eq(device), eq(command), any()))
                .thenReturn(Map.of("result", "ok"));

        receiver.commandReceive(new MqReceived<>(command("record-1"), Map.of(), false), ack);

        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(captor.getValue().resultValues()).containsEntry("result", "ok");
        verify(ack).ack();
    }

    @Test
    void failureBeforeRedeliveryReleasesDedupAndRequeues() throws Exception {
        when(dedupCache.tryAcquire("record-2")).thenReturn(true);
        when(deviceMetadata.getCache(10L)).thenReturn(null);

        receiver.commandReceive(new MqReceived<>(command("record-2"), Map.of(), false), ack);

        // A first-time failure must NOT send a result — the command is requeued and will
        // be retried, so reporting FAILED here would double-report on the redelivery.
        verify(driverSenderService, never()).commandResultSender(any());
        verify(dedupCache).release("record-2");
        verify(ack).reject(true);
    }

    @Test
    void redeliveryFailureSendsFailedResultWithoutRelease() throws Exception {
        when(dedupCache.tryAcquire("record-3")).thenReturn(true);
        when(deviceMetadata.getCache(10L)).thenReturn(null);
        receiver.commandReceive(new MqReceived<>(command("record-3"), Map.of(), true), ack);

        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.FAILED);
        verify(dedupCache, never()).release("record-3");
        verify(ack).ack();
    }

    @Test
    void duplicateCommandSendsDuplicateResult() throws Exception {
        when(dedupCache.tryAcquire("record-4")).thenReturn(false);

        receiver.commandReceive(new MqReceived<>(command("record-4"), Map.of(), false), ack);

        verifyNoInteractions(driverCustomService);
        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.DUPLICATE);
        verify(ack).ack();
    }

    @Test
    void invalidCommandIsRejected() throws Exception {
        receiver.commandReceive(new MqReceived<>(null, Map.of(), false), ack);

        verify(ack).reject(false);
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void missingTenantIdIsRejected() throws Exception {
        CommandCallDTO dto = CommandCallDTO.builder()
                .recordId("record-5")
                .deviceId(10L)
                .commandId(20L)
                .paramValues(Map.of("setpoint", "42"))
                .occurredAt(Instant.now())
                .expireAt(Instant.now().plusSeconds(10))
                .schemaVersion(1)
                .build();

        receiver.commandReceive(new MqReceived<>(dto, Map.of(), false), ack);

        verify(ack).reject(false);
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    private CommandCallDTO command(String recordId) {
        return CommandCallDTO.builder()
                .recordId(recordId)
                .tenantId(100L)
                .ownerNode("node-a")
                .fencingToken(77L)
                .deviceId(10L)
                .commandId(20L)
                .paramValues(Map.of("setpoint", "42"))
                .occurredAt(Instant.now())
                .expireAt(Instant.now().plusSeconds(10))
                .schemaVersion(1)
                .build();
    }
}
