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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointCommandLocalFacadeTest {

    @Mock
    private PointCommandService pointCommandService;

    private PointCommandLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new PointCommandLocalFacade(pointCommandService);
    }

    @Test
    void readPopulatesDeviceAndPointIdAndReturnsTrue() {
        when(pointCommandService.read(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(reactor.core.publisher.Mono.just("cmd-1"));
        String result = facade.submitRead(1L, 10L, 20L).block();
        assertThat(result).isEqualTo("cmd-1");

        ArgumentCaptor<PointCommandReadBO> captor = ArgumentCaptor.forClass(PointCommandReadBO.class);
        verify(pointCommandService).read(eq(1L), captor.capture());
        PointCommandReadBO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
    }

    @Test
    void writePopulatesDeviceIdPointIdAndValueAndReturnsTrue() {
        when(pointCommandService.write(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(reactor.core.publisher.Mono.just("cmd-2"));
        String result = facade.submitWrite(1L, 10L, 20L, "42").block();
        assertThat(result).isEqualTo("cmd-2");

        ArgumentCaptor<PointCommandWriteBO> captor = ArgumentCaptor.forClass(PointCommandWriteBO.class);
        verify(pointCommandService).write(eq(1L), captor.capture());
        PointCommandWriteBO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
        assertThat(passed.getValue()).isEqualTo("42");
    }
}
