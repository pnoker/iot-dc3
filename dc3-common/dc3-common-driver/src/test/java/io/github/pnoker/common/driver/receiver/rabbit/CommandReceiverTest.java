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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.entity.bo.AttributeBO;
import io.github.pnoker.common.driver.entity.bo.CommandRuntimeBO;
import io.github.pnoker.common.driver.entity.bo.DeviceBO;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DeviceMetadata;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.entity.dto.CommandCallDTO;
import io.github.pnoker.common.entity.dto.CommandCallResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.mq.listener.Acknowledgment;
import io.github.pnoker.common.mq.listener.MqReceived;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CommandReceiverTest {

    @Mock
    private DriverCustomService driverCustomService;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private DeviceMetadata deviceMetadata;

    @Spy
    private CommandDedupCache dedupCache;

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private Acknowledgment ack;

    private CommandReceiver receiver;
    private ThreadPoolExecutor commandExecutor;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.setNode("node-a");
        commandExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        receiver = new CommandReceiver(
                driverCustomService,
                driverSenderService,
                deviceMetadata,
                dedupCache,
                new DeviceLockManager(),
                driverMetadata,
                properties,
                commandExecutor);
        lenient().when(driverMetadata.getFencingToken(10L)).thenReturn(77L);
        lenient().when(driverSenderService.commandResultSender(any())).thenReturn(Mono.empty());
    }

    @AfterEach
    void tearDown() {
        commandExecutor.shutdownNow();
    }

    @Test
    void commandIsExecutedAndResultIsSent() throws Exception {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        CommandRuntimeBO command = commandRuntime();
        device.setCommandRuntimeIdMap(Map.of(20L, command));
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        Map<String, AttributeBO> driverConfig =
                Map.of("host", AttributeBO.builder().value("127.0.0.1").build());
        Map<String, AttributeBO> commandConfig =
                Map.of("address", AttributeBO.builder().value("A1").build());
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(driverConfig);
        when(deviceMetadata.getCommandConfig(10L, 20L)).thenReturn(commandConfig);
        when(driverCustomService.execute(eq(driverConfig), eq(commandConfig), eq(device), eq(command), any()))
                .thenReturn(Map.of("result", "ok"));

        await(receiver.commandReceive(new MqReceived<>(command("record-1"), Map.of(), false), ack));

        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(captor.getValue().resultValues()).containsEntry("result", "ok");
        verifyNoInteractions(ack);
    }

    @Test
    void failureBeforeRedeliveryReleasesDedupAndRequeues() throws Exception {
        when(deviceMetadata.getCache(10L)).thenReturn(null);

        StepVerifier.create(receiver.commandReceive(new MqReceived<>(command("record-2"), Map.of(), false), ack))
                .expectErrorMessage("Device not found in cache: 10")
                .verify();

        // A first-time failure must NOT send a result — the command is requeued and will
        // be retried, so reporting FAILED here would double-report on the redelivery.
        verify(driverSenderService, never()).commandResultSender(any());
        verify(dedupCache).release("command:record-2");
        verifyNoInteractions(ack);
    }

    @Test
    void redeliveryFailureSendsFailedResultWithoutRelease() throws Exception {
        when(deviceMetadata.getCache(10L)).thenReturn(null);
        await(receiver.commandReceive(new MqReceived<>(command("record-3"), Map.of(), true), ack));

        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.FAILED);
        verify(dedupCache, never()).release("command:record-3");
        verifyNoInteractions(ack);
    }

    @Test
    void missingCommandRuntimeMetadataReleasesDedupAndRequeues() throws Exception {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        device.setCommandRuntimeIdMap(Map.of());
        when(deviceMetadata.getCache(10L)).thenReturn(device);

        StepVerifier.create(receiver.commandReceive(
                        new MqReceived<>(command("record-missing-command"), Map.of(), false), ack))
                .expectErrorMessage("Command not found in device metadata: 20")
                .verify();

        verifyNoInteractions(driverCustomService);
        verify(driverSenderService, never()).commandResultSender(any());
        verify(dedupCache).release("command:record-missing-command");
        verifyNoInteractions(ack);
    }

    @Test
    void duplicateCommandSendsDuplicateResult() throws Exception {
        dedupCache.tryAcquire("command:record-4");

        await(receiver.commandReceive(new MqReceived<>(command("record-4"), Map.of(), false), ack));

        verifyNoInteractions(driverCustomService);
        ArgumentCaptor<CommandCallResultDTO> captor = ArgumentCaptor.forClass(CommandCallResultDTO.class);
        verify(driverSenderService).commandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.DUPLICATE);
        verifyNoInteractions(ack);
    }

    @Test
    void invalidCommandIsRejected() throws Exception {
        await(receiver.commandReceive(new MqReceived<>(null, Map.of(), false), ack));

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

        await(receiver.commandReceive(new MqReceived<>(dto, Map.of(), false), ack));

        verify(ack).reject(false);
        verifyNoInteractions(driverCustomService, driverSenderService);
    }

    @Test
    void receiptRetryDoesNotExecuteDeviceCommandTwice() throws Exception {
        DeviceBO device = new DeviceBO();
        device.setId(10L);
        CommandRuntimeBO command = commandRuntime();
        device.setCommandRuntimeIdMap(Map.of(20L, command));
        when(deviceMetadata.getCache(10L)).thenReturn(device);
        when(deviceMetadata.getDriverConfig(10L)).thenReturn(Map.of());
        when(deviceMetadata.getCommandConfig(10L, 20L)).thenReturn(Map.of());
        when(driverCustomService.execute(any(), any(), eq(device), eq(command), any()))
                .thenReturn(Map.of("result", "ok"));
        when(driverSenderService.commandResultSender(any()))
                .thenReturn(Mono.error(new IllegalStateException("broker unavailable")))
                .thenReturn(Mono.empty());

        StepVerifier.create(receiver.commandReceive(new MqReceived<>(command("record-6"), Map.of(), false), ack))
                .expectErrorMessage("broker unavailable")
                .verify();
        await(receiver.commandReceive(new MqReceived<>(command("record-6"), Map.of(), true), ack));

        verify(driverCustomService).execute(any(), any(), eq(device), eq(command), any());
        verify(driverSenderService, org.mockito.Mockito.times(2)).commandResultSender(any());
        verifyNoInteractions(ack);
        assertThat(dedupCache.result("command:record-6", CommandCallResultDTO.class))
                .map(CommandCallResultDTO::status)
                .contains(PointCommandStatusEnum.SUCCESS);
    }

    private void await(Mono<Void> completion) throws Exception {
        completion.toFuture().get(2, TimeUnit.SECONDS);
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

    private CommandRuntimeBO commandRuntime() {
        return new CommandRuntimeBO(20L, "Setpoint", "setpoint", null, null, 5, null, null, 1);
    }
}
