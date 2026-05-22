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
import io.github.pnoker.common.driver.service.DriverReadService;
import io.github.pnoker.common.driver.service.DriverSenderService;
import io.github.pnoker.common.driver.service.DriverWriteService;
import io.github.pnoker.common.entity.dto.PointCommandDTO;
import io.github.pnoker.common.enums.PointCommandTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    private Channel channel;

    private PointCommandReceiver receiver;
    private Message message;

    @BeforeEach
    void setUp() {
        receiver = new PointCommandReceiver(driverReadService, driverWriteService,
                driverSenderService, dedupCache);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(7L);
        message = new Message(new byte[0], props);
    }

    @Test
    void readCommandIsDispatchedToReadService() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.READ, "{}");
        dto.setCommandId("test-cmd-1");
        when(dedupCache.tryAcquire("test-cmd-1")).thenReturn(true);

        receiver.pointCommandReceive(channel, message, dto);

        verify(driverReadService).read(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void writeCommandIsDispatchedToWriteService() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.WRITE, "{}");
        dto.setCommandId("test-cmd-2");
        when(dedupCache.tryAcquire("test-cmd-2")).thenReturn(true);

        receiver.pointCommandReceive(channel, message, dto);

        verify(driverWriteService).write(dto);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void configCommandSendsFailedResult() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.CONFIG, "{}");
        dto.setCommandId("test-cmd-3");
        when(dedupCache.tryAcquire("test-cmd-3")).thenReturn(true);

        receiver.pointCommandReceive(channel, message, dto);

        verifyNoInteractions(driverReadService, driverWriteService);
        verify(driverSenderService).pointCommandResultSender(any(io.github.pnoker.common.entity.dto.PointCommandResultDTO.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsNullPayload() throws Exception {
        receiver.pointCommandReceive(channel, message, null);

        verify(channel).basicReject(eq(7L), eq(false));
        verify(channel, never()).basicAck(eq(7L), eq(false));
    }

    @Test
    void rejectsPayloadWithBlankContent() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.READ, "");
        receiver.pointCommandReceive(channel, message, dto);

        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void nacksAndRequeuesOnServiceFailure() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.READ, "{}");
        dto.setCommandId("test-cmd-4");
        when(dedupCache.tryAcquire("test-cmd-4")).thenReturn(true);
        doThrow(new RuntimeException("driver offline")).when(driverReadService).read(dto);

        receiver.pointCommandReceive(channel, message, dto);

        verify(channel).basicNack(eq(7L), eq(false), eq(true));
    }

    @Test
    void duplicateCommandSendsDuplicateResult() throws Exception {
        PointCommandDTO dto = new PointCommandDTO(PointCommandTypeEnum.READ, "{}");
        dto.setCommandId("dup-cmd");
        when(dedupCache.tryAcquire("dup-cmd")).thenReturn(false);

        receiver.pointCommandReceive(channel, message, dto);

        verifyNoInteractions(driverReadService, driverWriteService);
        verify(driverSenderService).pointCommandResultSender(
                any(io.github.pnoker.common.entity.dto.PointCommandResultDTO.class));
        verify(channel).basicAck(eq(7L), eq(false));
    }
}
