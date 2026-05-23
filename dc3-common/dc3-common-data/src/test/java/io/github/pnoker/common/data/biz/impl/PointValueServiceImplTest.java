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

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.alarm.AlarmRuleTriggerService;
import io.github.pnoker.common.data.cache.PointValueLocalCacheService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RepositoryException;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointValueServiceImplTest {

    @Mock
    private PointFacade pointFacade;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private PointValueLocalCacheService pointValueLocalCacheService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private AlarmRuleTriggerService alarmRuleTriggerService;

    @InjectMocks
    private PointValueServiceImpl service;

    private PointValueBO pv;

    private static Long eqLong(long value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }

    private static FacadeDeviceBO stubDevice(Long tenantId, Long profileId) {
        FacadeDeviceBO device = new FacadeDeviceBO();
        device.setTenantId(tenantId);
        device.setProfileId(profileId);
        return device;
    }

    private static FacadePointBO stubPoint(Long tenantId, Long profileId) {
        FacadePointBO point = new FacadePointBO();
        point.setTenantId(tenantId);
        point.setProfileId(profileId);
        return point;
    }

    @BeforeEach
    void setUp() {
        pv = PointValueBO.builder()
                .deviceId(10L)
                .pointId(20L)
                .build();
    }

    @Test
    void singleSaveIgnoresNullPayload() {
        assertThatNoException().isThrownBy(() -> service.save((PointValueBO) null));
        verify(pointValueLocalCacheService, never()).savePointValue(any(PointValueBO.class));
    }

    @Test
    void batchSaveIgnoresNullAndEmptyList() {
        assertThatNoException().isThrownBy(() -> service.save((List<PointValueBO>) null));
        assertThatNoException().isThrownBy(() -> service.save(List.of()));
        verify(pointValueLocalCacheService, never()).savePointValue(any(Long.class), any());
    }

    @Test
    void singleSaveStampsTimestampsAndPersistsToBothLayers() throws Exception {
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));

            service.save(pv);

            assertThat(pv.getCreateTime()).isNotNull();
            assertThat(pv.getOperateTime()).isNotNull();
            verify(pointValueLocalCacheService).savePointValue(pv);
            verify(repositoryService).savePointValue(pv);
            verify(alarmRuleTriggerService).processPointValue(pv);
        }
    }

    @Test
    void singleSavePreservesDriverProvidedCreateTime() {
        java.time.LocalDateTime driverStamp = java.time.LocalDateTime.of(2026, 1, 1, 12, 0, 0);
        pv.setCreateTime(driverStamp);

        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            service.save(pv);
            assertThat(pv.getCreateTime()).isEqualTo(driverStamp);
            assertThat(pv.getOperateTime()).isAfterOrEqualTo(driverStamp);
        }
    }

    @Test
    void batchSavePartitionsLargeListIntoChunksOfHundred() throws Exception {
        List<PointValueBO> batch = new java.util.ArrayList<>();
        for (int i = 0; i < 250; i++) {
            batch.add(PointValueBO.builder().deviceId(10L).pointId((long) i).build());
        }

        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            service.save(batch);
            // 250 entries -> 100 + 100 + 50 = 3 chunks
            verify(repositoryService, times(3)).savePointValues(any());
            verify(pointValueLocalCacheService).savePointValue(eqLong(10L), any());
            // PointValueServiceImpl.save(List) hands the whole batch to the trigger
            // in one call now; the trigger's own contract is responsible for the
            // per-element fan-out (covered in AlarmRuleTriggerServiceImplTest).
            verify(alarmRuleTriggerService).processPointValues(batch);
        }
    }

    @Test
    void singleSaveSwallowsRepositoryFailures() throws Exception {
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            org.mockito.Mockito.doThrow(new RuntimeException("downstream offline"))
                    .when(repositoryService).savePointValue(any(PointValueBO.class));

            assertThatNoException().isThrownBy(() -> service.save(pv));
        }
    }

    @Test
    void historyShortCircuitsForBlankInputs() {
        assertThat(service.history(null, 1L, 2L, 10)).isEmpty();
        assertThat(service.history(1L, null, 2L, 10)).isEmpty();
        assertThat(service.history(1L, 2L, null, 10)).isEmpty();
    }

    @Test
    void historyClampsCountToBetweenOneAndFiveHundred() {
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            FacadeDeviceBO device = stubDevice(1L, 5L);
            FacadePointBO point = stubPoint(1L, 5L);
            when(deviceFacade.getById(1L, 2L)).thenReturn(device);
            when(pointFacade.getById(1L, 3L)).thenReturn(point);

            service.history(1L, 2L, 3L, 0);
            verify(repositoryService).listHistoryPointValue(1L, 2L, 3L, 100);

            service.history(1L, 2L, 3L, 1000);
            verify(repositoryService).listHistoryPointValue(1L, 2L, 3L, 500);

            service.history(1L, 2L, 3L, 250);
            verify(repositoryService).listHistoryPointValue(1L, 2L, 3L, 250);
        }
    }

    @Test
    void historyRejectsCrossTenantDevice() {
        when(deviceFacade.getById(1L, 2L)).thenReturn(null);
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            assertThatThrownBy(() -> service.history(1L, 2L, 3L, 10))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Device");
        }
    }

    @Test
    void historyRejectsPointThatDoesNotBelongToDeviceProfile() {
        FacadeDeviceBO device = stubDevice(1L, 5L);
        FacadePointBO point = stubPoint(1L, 99L); // profileId mismatch
        when(deviceFacade.getById(1L, 2L)).thenReturn(device);
        when(pointFacade.getById(1L, 3L)).thenReturn(point);

        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            assertThatThrownBy(() -> service.history(1L, 2L, 3L, 10))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Point");
        }
    }

    @Test
    void getFirstRepositoryServiceFailsLoudWhenMultipleConfigured() {
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get)
                    .thenReturn(List.of(repositoryService, Mockito.mock(RepositoryService.class)));

            assertThatThrownBy(() -> service.history(1L, 0L, 0L, 10))
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("multiple repository");
        }
    }

    @Test
    void getFirstRepositoryServiceFailsLoudWhenNoneConfigured() {
        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of());
            assertThatThrownBy(() -> service.history(1L, 0L, 0L, 10))
                    .isInstanceOf(RepositoryException.class)
                    .hasMessageContaining("at least one");
        }
    }

    @Test
    void latestReturnsPlaceholderForBoundPointWithoutValue() {
        Pages pages = new Pages();
        pages.setCurrent(1);
        pages.setSize(10);

        PointValueQuery query = new PointValueQuery();
        query.setPage(pages);
        query.setTenantId(1L);
        query.setDeviceId(10L);

        FacadePointBO pointWithValue = stubPoint(1L, 5L);
        pointWithValue.setId(20L);
        FacadePointBO pointWithoutValue = stubPoint(1L, 5L);
        pointWithoutValue.setId(21L);

        PointValueBO cached = PointValueBO.builder()
                .tenantId(1L)
                .deviceId(10L)
                .pointId(20L)
                .rawValue("42")
                .calValue("42")
                .build();

        when(deviceFacade.getById(1L, 10L)).thenReturn(stubDevice(1L, 5L));
        when(pointFacade.listByPage(any())).thenReturn(new FacadePage<>(1, 10, 2, 1,
                List.of(pointWithValue, pointWithoutValue)));
        when(pointValueLocalCacheService.selectLatestPointValue(1L, 10L, List.of(20L, 21L)))
                .thenReturn(Map.of(20L, cached));

        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));

            Page<PointValueBO> result = service.latest(query);

            assertThat(result.getTotal()).isEqualTo(2);
            assertThat(result.getRecords()).hasSize(2);
            assertThat(result.getRecords().get(0)).isSameAs(cached);
            assertThat(result.getRecords().get(0).getHasLatestValue()).isTrue();
            PointValueBO placeholder = result.getRecords().get(1);
            assertThat(placeholder.getTenantId()).isEqualTo(1L);
            assertThat(placeholder.getDeviceId()).isEqualTo(10L);
            assertThat(placeholder.getPointId()).isEqualTo(21L);
            assertThat(placeholder.getRawValue()).isEqualTo(DataConstant.PointValue.NO_LATEST_VALUE);
            assertThat(placeholder.getCalValue()).isEqualTo(DataConstant.PointValue.NO_LATEST_VALUE);
            assertThat(placeholder.getHasLatestValue()).isFalse();
            assertThat(placeholder.getCreateTime()).isNull();
            assertThat(placeholder.getOperateTime()).isNull();
        }

        verify(repositoryService, never()).selectLatestPointValue(1L, 10L, 20L);
        verify(repositoryService).selectLatestPointValue(1L, 10L, 21L);
    }

    @Test
    void pageDelegatesToRepositoryService() {
        FacadeDeviceBO device = stubDevice(1L, 5L);
        FacadePointBO point = stubPoint(1L, 5L);
        when(deviceFacade.getById(1L, 2L)).thenReturn(device);
        when(pointFacade.getById(1L, 3L)).thenReturn(point);

        PointValueQuery query = new PointValueQuery();
        query.setTenantId(1L);
        query.setDeviceId(2L);
        query.setPointId(3L);

        try (MockedStatic<RepositoryStrategyFactory> factory =
                     Mockito.mockStatic(RepositoryStrategyFactory.class)) {
            factory.when(RepositoryStrategyFactory::get).thenReturn(List.of(repositoryService));
            service.page(query);
            verify(repositoryService).listPagePointValue(query);
        }
    }
}
