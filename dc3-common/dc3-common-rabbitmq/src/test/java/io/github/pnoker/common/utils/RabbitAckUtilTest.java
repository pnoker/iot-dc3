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

package io.github.pnoker.common.utils;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitAckUtilTest {

    @Mock
    private Channel channel;

    @Test
    void ackInvokesBasicAckWithMultipleFalse() throws IOException {
        RabbitAckUtil.ack(channel, 7L);
        verify(channel).basicAck(eq(7L), eq(false));
    }

    @Test
    void ackPropagatesIoExceptionFromBroker() throws IOException {
        doThrow(new IOException("broker gone")).when(channel).basicAck(eq(7L), eq(false));
        assertThatThrownBy(() -> RabbitAckUtil.ack(channel, 7L)).isInstanceOf(IOException.class);
    }

    @Test
    void rejectInvokesBasicRejectWithRequeueFalse() throws IOException {
        RabbitAckUtil.reject(channel, 7L);
        verify(channel).basicReject(eq(7L), eq(false));
    }

    @Test
    void rejectSwallowsIoExceptionToKeepConsumerRunning() throws IOException {
        doThrow(new IOException("broker gone")).when(channel).basicReject(eq(7L), eq(false));
        // The receiver loop must not crash because the broker dropped — surface as a no-op.
        assertThatNoException().isThrownBy(() -> RabbitAckUtil.reject(channel, 7L));
    }

    @Test
    void nackForwardsRequeueFlag() throws IOException {
        RabbitAckUtil.nack(channel, 7L, true);
        verify(channel).basicNack(eq(7L), eq(false), eq(true));
        RabbitAckUtil.nack(channel, 8L, false);
        verify(channel).basicNack(eq(8L), eq(false), eq(false));
    }

    @Test
    void nackSwallowsIoExceptionToKeepConsumerRunning() throws IOException {
        doThrow(new IOException("broker gone")).when(channel).basicNack(eq(7L), eq(false), eq(true));
        assertThatNoException().isThrownBy(() -> RabbitAckUtil.nack(channel, 7L, true));
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<RabbitAckUtil> constructor = RabbitAckUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
