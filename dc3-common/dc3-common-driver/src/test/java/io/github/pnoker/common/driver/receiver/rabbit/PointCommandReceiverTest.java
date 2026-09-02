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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import io.github.pnoker.common.driver.metadata.DriverMetadata;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandPayload;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
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
class PointCommandReceiverTest {

    @Mock
    private DriverReadService driverReadService;

    @Mock
    private DriverWriteService driverWriteService;

    @Mock
    private DriverSenderService driverSenderService;

    @Spy
    private CommandDedupCache dedupCache;

    @Mock
    private DriverMetadata driverMetadata;

    @Mock
    private Acknowledgment ack;

    private PointCommandReceiver receiver;
    private ThreadPoolExecutor commandExecutor;

    @BeforeEach
    void setUp() {
        DriverProperties properties = new DriverProperties();
        properties.setNode("node-a");
        commandExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        receiver = new PointCommandReceiver(
                driverReadService,
                driverWriteService,
                driverSenderService,
                dedupCache,
                new DeviceLockManager(),
                driverMetadata,
                properties,
                commandExecutor);
        lenient().when(driverMetadata.getFencingToken(10L)).thenReturn(77L);
        lenient().when(driverSenderService.pointCommandResultSender(any())).thenReturn(Mono.empty());
    }

    @AfterEach
    void tearDown() {
        commandExecutor.shutdownNow();
    }

    @Test
    void readCommandCompletesAfterResultConfirmation() {
        StepVerifier.create(receiver.pointCommandReceive(received(readCommand("test-cmd-1"), false), ack))
                .verifyComplete();

        verify(driverReadService).read(10L, 20L);
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(captor.getValue().responseValue()).isNull();
        assertThat(dedupCache.result("test-cmd-1", PointCommandResultDTO.class)).contains(captor.getValue());
        verifyNoInteractions(ack);
    }

    @Test
    void writeCommandCompletesAfterResultConfirmation() {
        when(driverWriteService.write(10L, 20L, "42")).thenReturn(true);

        StepVerifier.create(receiver.pointCommandReceive(received(writeCommand("test-cmd-2"), false), ack))
                .verifyComplete();

        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(captor.getValue().responseValue()).isEqualTo("42");
        verifyNoInteractions(ack);
    }

    @Test
    void rejectsInvalidEnvelopeAsDeadLetter() {
        StepVerifier.create(receiver.pointCommandReceive(received(null, false), ack))
                .verifyComplete();

        verify(ack).reject(false);
        verifyNoInteractions(driverReadService, driverWriteService, driverSenderService);
    }

    @Test
    void rejectsInvalidPayloadAsDeadLetter() {
        PointCommandDTO command = new PointCommandDTO(
                "bad-read",
                100L,
                "node-a",
                77L,
                PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(null, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP,
                null,
                Instant.now(),
                Instant.now().plusSeconds(10),
                1);

        StepVerifier.create(receiver.pointCommandReceive(received(command, false), ack))
                .verifyComplete();

        verify(ack).reject(false);
        verifyNoInteractions(driverReadService, driverWriteService, driverSenderService);
    }

    @Test
    void firstExecutionFailurePropagatesAndReleasesDedup() {
        doThrow(new IllegalStateException("driver offline"))
                .when(driverReadService)
                .read(10L, 20L);

        StepVerifier.create(receiver.pointCommandReceive(received(readCommand("test-cmd-4"), false), ack))
                .expectErrorMessage("driver offline")
                .verify();

        assertThat(dedupCache.tryAcquire("test-cmd-4")).isTrue();
        verify(driverSenderService, never()).pointCommandResultSender(any());
        verifyNoInteractions(ack);
    }

    @Test
    void redeliveryExecutionFailurePublishesTerminalFailure() {
        doThrow(new IllegalStateException("driver offline"))
                .when(driverReadService)
                .read(10L, 20L);

        StepVerifier.create(receiver.pointCommandReceive(received(readCommand("test-cmd-5"), true), ack))
                .verifyComplete();

        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.FAILED);
        assertThat(captor.getValue().errorCode()).isEqualTo("DRIVER_ERROR");
        assertThat(dedupCache.result("test-cmd-5", PointCommandResultDTO.class)).contains(captor.getValue());
    }

    @Test
    void duplicateInProgressCommandPublishesDuplicateResult() {
        dedupCache.tryAcquire("dup-cmd");

        StepVerifier.create(receiver.pointCommandReceive(received(readCommand("dup-cmd"), false), ack))
                .verifyComplete();

        verifyNoInteractions(driverReadService, driverWriteService);
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.DUPLICATE);
        assertThat(captor.getValue().errorCode()).isEqualTo("DUPLICATE");
    }

    @Test
    void expiredCommandPublishesExpiredResultWithoutDedupAcquisition() {
        PointCommandDTO expired = new PointCommandDTO(
                "exp-cmd",
                100L,
                "node-a",
                77L,
                PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP,
                null,
                Instant.now().minusSeconds(60),
                Instant.now().minusSeconds(30),
                1);

        StepVerifier.create(receiver.pointCommandReceive(received(expired, false), ack))
                .verifyComplete();

        verifyNoInteractions(driverReadService, driverWriteService);
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.EXPIRED);
        assertThat(captor.getValue().errorCode()).isEqualTo("EXPIRED");
        assertThat(dedupCache.result("exp-cmd", PointCommandResultDTO.class)).isEmpty();
    }

    @Test
    void failedWritePublishesFailedResult() {
        when(driverWriteService.write(10L, 20L, "42")).thenReturn(false);

        StepVerifier.create(receiver.pointCommandReceive(received(writeCommand("write-fail"), false), ack))
                .verifyComplete();

        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.FAILED);
        assertThat(captor.getValue().errorCode()).isEqualTo("WRITE_FAILED");
    }

    @Test
    void receiptRetryDoesNotExecuteDeviceCommandTwice() {
        when(driverWriteService.write(10L, 20L, "42")).thenReturn(true);
        when(driverSenderService.pointCommandResultSender(any()))
                .thenReturn(Mono.error(new IllegalStateException("broker unavailable")))
                .thenReturn(Mono.empty());
        PointCommandDTO command = writeCommand("retry-result");

        StepVerifier.create(receiver.pointCommandReceive(received(command, false), ack))
                .expectErrorMessage("broker unavailable")
                .verify();
        StepVerifier.create(receiver.pointCommandReceive(received(command, true), ack))
                .verifyComplete();

        verify(driverWriteService).write(10L, 20L, "42");
        verify(driverSenderService, times(2)).pointCommandResultSender(any());
        assertThat(dedupCache.result("retry-result", PointCommandResultDTO.class))
                .map(PointCommandResultDTO::status)
                .contains(PointCommandStatusEnum.SUCCESS);
        verifyNoInteractions(ack);
    }

    @Test
    void staleOwnerPublishesFailureWithoutDeviceExecution() {
        when(driverMetadata.getFencingToken(10L)).thenReturn(78L);

        StepVerifier.create(receiver.pointCommandReceive(received(readCommand("stale"), false), ack))
                .verifyComplete();

        verifyNoInteractions(driverReadService, driverWriteService);
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().errorCode()).isEqualTo("STALE_OWNER");
    }

    private MqReceived<PointCommandDTO> received(PointCommandDTO command, boolean redelivered) {
        return new MqReceived<>(command, Map.of(), redelivered);
    }

    private PointCommandDTO readCommand(String commandId) {
        return new PointCommandDTO(
                commandId,
                100L,
                "node-a",
                77L,
                PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP,
                null,
                Instant.now(),
                Instant.now().plusSeconds(10),
                1);
    }

    private PointCommandDTO writeCommand(String commandId) {
        return new PointCommandDTO(
                commandId,
                100L,
                "node-a",
                77L,
                PointCommandTypeEnum.WRITE,
                new PointCommandPayload.WritePayload(10L, 20L, "42"),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP,
                null,
                Instant.now(),
                Instant.now().plusSeconds(10),
                1);
    }
}
