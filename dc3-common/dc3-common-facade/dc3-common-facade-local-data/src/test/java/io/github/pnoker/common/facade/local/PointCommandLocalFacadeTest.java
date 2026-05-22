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
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

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
        boolean result = facade.submitRead(1L, 10L, 20L);
        assertThat(result).isTrue();

        ArgumentCaptor<PointCommandReadVO> captor = ArgumentCaptor.forClass(PointCommandReadVO.class);
        verify(pointCommandService).read(eq(1L), captor.capture());
        PointCommandReadVO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
    }

    @Test
    void writePopulatesDeviceIdPointIdAndValueAndReturnsTrue() {
        boolean result = facade.submitWrite(1L, 10L, 20L, "42");
        assertThat(result).isTrue();

        ArgumentCaptor<PointCommandWriteVO> captor = ArgumentCaptor.forClass(PointCommandWriteVO.class);
        verify(pointCommandService).write(eq(1L), captor.capture());
        PointCommandWriteVO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
        assertThat(passed.getValue()).isEqualTo("42");
    }
}
