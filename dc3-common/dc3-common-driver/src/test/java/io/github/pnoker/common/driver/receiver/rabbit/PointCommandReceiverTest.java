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

import com.rabbitmq.client.Channel;
import io.github.pnoker.common.driver.command.CommandDedupCache;
import io.github.pnoker.common.driver.command.DeviceLockManager;
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.entity.dto.PointCommandPayload;
import io.github.pnoker.common.entity.dto.PointCommandResultDTO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointCommandReceiverTest {

    @Mock
    private DriverReadService driverReadService;

    @Mock
    private DriverWriteService driverWriteService;

    @Mock
    private DriverSenderService driverSenderService;

    @Mock
    private CommandDedupCache dedupCache;

    @Mock
    private DeviceLockManager deviceLockManager;

    @Mock
    private Channel channel;

    private PointCommandReceiver receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        receiver = new PointCommandReceiver(driverReadService, driverWriteService,
                driverSenderService, dedupCache, deviceLockManager);

        // DeviceLockManager executes the supplier inline
        lenient().when(deviceLockManager.runExclusive(anyLong(), ArgumentMatchers.<Supplier<String>>any()))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(1)).get());

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    private PointCommandDTO readCommand(String commandId) {
        return new PointCommandDTO(commandId, 100L, PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now(), Instant.now().plusSeconds(10), 1);
    }

    private PointCommandDTO writeCommand(String commandId) {
        return new PointCommandDTO(commandId, 100L, PointCommandTypeEnum.WRITE,
                new PointCommandPayload.WritePayload(10L, 20L, "42"),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now(), Instant.now().plusSeconds(10), 1);
    }

    @Test
    void readCommandIsDispatchedToReadServiceAndSendsSuccessResult() throws Exception {
        when(dedupCache.tryAcquire("test-cmd-1")).thenReturn(true);

        receiver.pointCommandReceive(channel, message, readCommand("test-cmd-1"));

        verify(driverReadService).read(eq(10L), eq(20L));
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        assertThat(captor.getValue().commandId()).isEqualTo("test-cmd-1");
        // read returns no value, so responseValue stays null
        assertThat(captor.getValue().responseValue()).isNull();
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void writeCommandIsDispatchedToWriteServiceAndSendsSuccessWithValue() throws Exception {
        when(dedupCache.tryAcquire("test-cmd-2")).thenReturn(true);
        when(driverWriteService.write(eq(10L), eq(20L), eq("42"))).thenReturn(true);

        receiver.pointCommandReceive(channel, message, writeCommand("test-cmd-2"));

        verify(driverWriteService).write(eq(10L), eq(20L), eq("42"));
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.SUCCESS);
        // a successful write echoes the written value back as the responseValue
        assertThat(captor.getValue().responseValue()).isEqualTo("42");
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.pointCommandReceive(channel, message, null);

        verify(channel).basicReject(eq(7L), eq(false));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithNullType() throws Exception {
        PointCommandDTO dto = new PointCommandDTO("id", 100L, null,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now(), Instant.now().plusSeconds(10), 1);
        receiver.pointCommandReceive(channel, message, dto);

        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithNullCommandId() throws Exception {
        receiver.pointCommandReceive(channel, message, readCommand(null));

        verify(channel).basicReject(eq(7L), eq(false));
        verifyNoInteractions(driverReadService, driverWriteService);
    }

    @Test
    void rejectsPayloadWithNullTenantId() throws Exception {
        PointCommandDTO dto = new PointCommandDTO("id", null, PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now(), Instant.now().plusSeconds(10), 1);

        receiver.pointCommandReceive(channel, message, dto);

        verify(channel).basicReject(eq(7L), eq(false));
        verifyNoInteractions(driverReadService, driverWriteService);
    }

    @Test
    void nacksAndRequeuesOnServiceFailure() throws Exception {
        when(dedupCache.tryAcquire("test-cmd-4")).thenReturn(true);
        doThrow(new RuntimeException("driver offline")).when(driverReadService).read(anyLong(), anyLong());

        receiver.pointCommandReceive(channel, message, readCommand("test-cmd-4"));

        // A first-time failure must NOT send a result — the command is requeued and will
        // be retried, so reporting FAILED here would double-report on the redelivery.
        verify(driverSenderService, never()).pointCommandResultSender(any());
        verify(dedupCache).release("test-cmd-4");
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
    }

    @Test
    void rejectsReadPayloadWithNullDeviceId() throws Exception {
        PointCommandDTO dto = new PointCommandDTO("bad-read", 100L, PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(null, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now(), Instant.now().plusSeconds(10), 1);

        receiver.pointCommandReceive(channel, message, dto);

        verify(channel).basicReject(eq(7L), eq(false));
        verifyNoInteractions(driverReadService, driverWriteService);
    }

    @Test
    void duplicateCommandSendsDuplicateResult() throws Exception {
        when(dedupCache.tryAcquire("dup-cmd")).thenReturn(false);

        receiver.pointCommandReceive(channel, message, readCommand("dup-cmd"));

        verifyNoInteractions(driverReadService, driverWriteService);
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.DUPLICATE);
        assertThat(captor.getValue().errorCode()).isEqualTo("DUPLICATE");
        assertThat(captor.getValue().commandId()).isEqualTo("dup-cmd");
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void expiredCommandSendsExpiredResult() throws Exception {
        PointCommandDTO expired = new PointCommandDTO("exp-cmd", 100L, PointCommandTypeEnum.READ,
                new PointCommandPayload.ReadPayload(10L, 20L),
                io.github.pnoker.common.enums.PointCommandSourceEnum.HTTP, null,
                Instant.now().minusSeconds(60), Instant.now().minusSeconds(30), 1);

        receiver.pointCommandReceive(channel, message, expired);

        verifyNoInteractions(driverReadService, driverWriteService);
        // expired commands must be rejected before touching the dedup cache
        verify(dedupCache, never()).tryAcquire(any());
        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.EXPIRED);
        assertThat(captor.getValue().errorCode()).isEqualTo("EXPIRED");
        assertThat(captor.getValue().commandId()).isEqualTo("exp-cmd");
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void failedWriteSendsFailedResult() throws Exception {
        when(dedupCache.tryAcquire("write-fail")).thenReturn(true);
        when(driverWriteService.write(eq(10L), eq(20L), eq("42"))).thenReturn(false);

        receiver.pointCommandReceive(channel, message, writeCommand("write-fail"));

        ArgumentCaptor<PointCommandResultDTO> captor = ArgumentCaptor.forClass(PointCommandResultDTO.class);
        verify(driverSenderService).pointCommandResultSender(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(PointCommandStatusEnum.FAILED);
        assertThat(captor.getValue().errorCode()).isEqualTo("WRITE_FAILED");
        assertThat(captor.getValue().responseValue()).isNull();
        verify(channel).basicAck(eq(7L), eq(false));
    }
}
