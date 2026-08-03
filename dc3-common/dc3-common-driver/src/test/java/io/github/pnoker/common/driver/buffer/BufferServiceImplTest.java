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

package io.github.pnoker.common.driver.buffer;

import io.github.pnoker.common.driver.entity.bean.PointValue;
import io.github.pnoker.common.driver.entity.property.DriverProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

/**
 * Verifies the buffer service offer/republish lifecycle with a mocked RabbitTemplate.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.6.2
 */
@ExtendWith(MockitoExtension.class)
class BufferServiceImplTest {

    @TempDir
    Path tmp;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DriverProperties properties;
    private BufferServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new DriverProperties();
        properties.getBuffer().setEnabled(true);
        properties.getBuffer().setDbPath(tmp.resolve("buffer.db").toString());
        properties.getBuffer().setBatchSize(10);
        properties.getBuffer().setMaxRetry(3);
        properties.getBuffer().setBackoffSeconds(0);
        service = new BufferServiceImpl(properties, rabbitTemplate);
        service.initialize();
    }

    @Test
    void offerPersistsAndRepublishSendsThenDrains() {
        service.offer(pointValue(), "rk", "id-1", 1);
        assertThat(service.pendingCount()).as("offer should persist one record").isEqualTo(1);

        service.republishBatch();
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
        assertThat(service.pendingCount()).as("republish should drain the buffer").isEqualTo(0);

        reset(rabbitTemplate);
        service.republishBatch();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
    }

    @Test
    void republishRequeuesOnAmqpExceptionThenResends() {
        service.offer(pointValue(), "rk", "id-1", 1);

        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
        service.republishBatch();

        reset(rabbitTemplate);
        service.republishBatch();
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));

        reset(rabbitTemplate);
        service.republishBatch();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
    }

    @Test
    void disabledBufferIsNoOp() {
        DriverProperties disabledProps = new DriverProperties();
        disabledProps.getBuffer().setEnabled(false);
        disabledProps.getBuffer().setDbPath(tmp.resolve("disabled.db").toString());
        BufferServiceImpl disabled = new BufferServiceImpl(disabledProps, rabbitTemplate);
        disabled.initialize();

        assertThat(disabled.isEnabled()).isFalse();
        disabled.offer(pointValue(), "rk", "id-1", 1);
        disabled.republishBatch();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(PointValue.class), any(CorrelationData.class));
    }

    private PointValue pointValue() {
        return PointValue.builder().deviceId(1L).pointId(2L).rawValue("42").build();
    }
}
