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

import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
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
class PointValueCommandLocalFacadeTest {

    @Mock
    private PointValueCommandService pointValueCommandService;

    private PointValueCommandLocalFacade facade;

    @BeforeEach
    void setUp() {
        facade = new PointValueCommandLocalFacade(pointValueCommandService);
    }

    @Test
    void readPopulatesDeviceAndPointIdAndReturnsTrue() {
        boolean result = facade.dispatchRead(1L, 10L, 20L);
        assertThat(result).isTrue();

        ArgumentCaptor<PointValueReadVO> captor = ArgumentCaptor.forClass(PointValueReadVO.class);
        verify(pointValueCommandService).read(eq(1L), captor.capture());
        PointValueReadVO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
    }

    @Test
    void writePopulatesDeviceIdPointIdAndValueAndReturnsTrue() {
        boolean result = facade.dispatchWrite(1L, 10L, 20L, "42");
        assertThat(result).isTrue();

        ArgumentCaptor<PointValueWriteVO> captor = ArgumentCaptor.forClass(PointValueWriteVO.class);
        verify(pointValueCommandService).write(eq(1L), captor.capture());
        PointValueWriteVO passed = captor.getValue();
        assertThat(passed.getDeviceId()).isEqualTo(10L);
        assertThat(passed.getPointId()).isEqualTo(20L);
        assertThat(passed.getValue()).isEqualTo("42");
    }
}
