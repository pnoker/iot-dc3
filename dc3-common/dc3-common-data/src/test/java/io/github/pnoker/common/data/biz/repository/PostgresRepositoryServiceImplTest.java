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
package io.github.pnoker.common.data.biz.repository;

import io.github.pnoker.common.data.dal.PointValueManager;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.exception.AddException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PostgresRepositoryServiceImpl} verifying that
 * numValue set by the driver is passed through to the DAL layer unchanged.
 */
@ExtendWith(MockitoExtension.class)
class PostgresRepositoryServiceImplTest {

    @Mock
    private PointValueBuilder pointValueBuilder;

    @Mock
    private PointValueManager pointValueManager;

    @InjectMocks
    private PostgresRepositoryServiceImpl service;

    @Captor
    private ArgumentCaptor<PointValueDO> doCaptor;

    private PointValueBO numericBO;
    private PointValueBO stringBO;
    private PointValueDO numericDO;
    private PointValueDO stringDO;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        numericBO = PointValueBO.builder()
                .deviceId(1L).pointId(10L).tenantId(100L)
                .rawValue("42").calValue("42.5").numValue(42.5)
                .createTime(now).operateTime(now)
                .build();

        stringBO = PointValueBO.builder()
                .deviceId(2L).pointId(20L).tenantId(100L)
                .rawValue("on").calValue("on").numValue(null)
                .createTime(now).operateTime(now)
                .build();

        numericDO = new PointValueDO();
        numericDO.setDeviceId(1L);
        numericDO.setPointId(10L);
        numericDO.setTenantId(100L);
        numericDO.setRawValue("42");
        numericDO.setCalValue("42.5");
        numericDO.setNumValue(42.5);

        stringDO = new PointValueDO();
        stringDO.setDeviceId(2L);
        stringDO.setPointId(20L);
        stringDO.setTenantId(100L);
        stringDO.setRawValue("on");
        stringDO.setCalValue("on");
        stringDO.setNumValue(null);
    }

    @Test
    void savePointValuePassesNumericValueThrough() {
        when(pointValueBuilder.buildDOByBO(numericBO)).thenReturn(numericDO);
        when(pointValueManager.save(numericDO)).thenReturn(true);

        service.savePointValue(numericBO);

        verify(pointValueBuilder).buildDOByBO(numericBO);
        verify(pointValueManager).save(doCaptor.capture());
        assertThat(doCaptor.getValue().getNumValue()).isEqualTo(42.5);
    }

    @Test
    void savePointValuePassesNullNumValueThrough() {
        when(pointValueBuilder.buildDOByBO(stringBO)).thenReturn(stringDO);
        when(pointValueManager.save(stringDO)).thenReturn(true);

        service.savePointValue(stringBO);

        verify(pointValueManager).save(doCaptor.capture());
        assertThat(doCaptor.getValue().getNumValue()).isNull();
    }

    @Test
    void savePointValueThrowsOnFailure() {
        when(pointValueBuilder.buildDOByBO(numericBO)).thenReturn(numericDO);
        when(pointValueManager.save(numericDO)).thenReturn(false);

        assertThatThrownBy(() -> service.savePointValue(numericBO))
                .isInstanceOf(AddException.class);
    }

    @Test
    void savePointValuesBatchPassesNumericValuesThrough() {
        when(pointValueBuilder.buildDOListByBOList(anyList())).thenReturn(java.util.List.of(numericDO, stringDO));
        when(pointValueManager.saveBatch(anyList())).thenReturn(true);

        service.savePointValues(java.util.List.of(numericBO, stringBO));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<PointValueDO>> listCaptor = ArgumentCaptor.forClass(java.util.List.class);
        verify(pointValueManager).saveBatch(listCaptor.capture());
        java.util.List<PointValueDO> saved = listCaptor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getNumValue()).isEqualTo(42.5);
        assertThat(saved.get(1).getNumValue()).isNull();
    }

    @Test
    void noApplyNumericProjectionCalled() {
        // Verify the service does NOT modify numValue after builder conversion.
        // Before the refactoring, applyNumericProjection would overwrite numValue.
        // Now the driver sets it, and we pass it through unchanged.
        PointValueDO captured = new PointValueDO();
        captured.setNumValue(99.9); // driver-set value
        when(pointValueBuilder.buildDOByBO(any())).thenReturn(captured);
        when(pointValueManager.save(any())).thenReturn(true);

        PointValueBO bo = PointValueBO.builder()
                .deviceId(1L).pointId(10L).tenantId(100L)
                .rawValue("99.9").calValue("99.9").numValue(99.9)
                .build();

        service.savePointValue(bo);

        verify(pointValueManager).save(doCaptor.capture());
        // numValue should be exactly what the driver set, not re-parsed
        assertThat(doCaptor.getValue().getNumValue()).isEqualTo(99.9);
    }
}
