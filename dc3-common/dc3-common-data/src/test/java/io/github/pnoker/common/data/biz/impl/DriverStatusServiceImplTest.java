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

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import io.github.pnoker.common.data.dal.EntityStateManager;
import io.github.pnoker.common.data.entity.model.EntityStateDO;
import io.github.pnoker.common.data.entity.query.DriverQuery;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.api.DriverFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverStatusServiceImplTest {

    @Mock
    private DriverFacade driverFacade;

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @InjectMocks
    private DriverStatusServiceImpl service;

    private FacadeDriverBO driver(Long id) {
        FacadeDriverBO bo = new FacadeDriverBO();
        bo.setId(id);
        return bo;
    }

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO bo = new FacadeDeviceBO();
        bo.setId(id);
        return bo;
    }

    private EntityStateDO onlineState(Long entityId, int typeFlag) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) typeFlag);
        state.setEntityId(entityId);
        state.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        state.setExpireTime(LocalDateTime.now().plusSeconds(60));
        return state;
    }

    private EntityStateDO expiredState(Long entityId, int typeFlag) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) typeFlag);
        state.setEntityId(entityId);
        state.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        state.setExpireTime(LocalDateTime.now().minusSeconds(10));
        return state;
    }

    @Test
    void getStatusByPageReturnsEmptyMapForEmptyPage() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of());
        when(driverFacade.listByPage(any())).thenReturn(page);
        assertThat(service.getStatusByPage(new DriverQuery())).isEmpty();
    }

    @Test
    void getStatusByPageDefaultsToOfflineWhenDbRowMissing() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of(driver(1L)));
        when(driverFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        assertThat(service.getStatusByPage(new DriverQuery()))
                .containsEntry(1L, EntityStatusEnum.OFFLINE.getCode());
    }

    @Test
    void getStatusByPageReturnsOnlineFromDb() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of(driver(1L)));
        when(driverFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(onlineState(1L, EntityTypeEnum.DRIVER.getIndex()));
        assertThat(service.getStatusByPage(new DriverQuery()))
                .containsEntry(1L, EntityStatusEnum.ONLINE.getCode());
    }

    @Test
    void getStatusByPageReturnsOfflineWhenExpired() {
        FacadePage<FacadeDriverBO> page = new FacadePage<>();
        page.setRecords(List.of(driver(1L)));
        when(driverFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(expiredState(1L, EntityTypeEnum.DRIVER.getIndex()));
        assertThat(service.getStatusByPage(new DriverQuery()))
                .containsEntry(1L, EntityStatusEnum.OFFLINE.getCode());
    }

    @Test
    void getDeviceOnlineByDriverIdReturnsZeroWhenDriverMissing() {
        when(driverFacade.getById(1L, 7L)).thenReturn(null);
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo(0L);
    }

    @Test
    void getDeviceOnlineByDriverIdReturnsZeroWhenNoDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of());
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo(0L);
    }

    @Test
    void getDeviceOnlineByDriverIdCountsOnlineDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of(device(10L), device(11L)));
        EntityStateDO online10 = onlineState(10L, EntityTypeEnum.DEVICE.getIndex());
        online10.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        EntityStateDO expired11 = expiredState(11L, EntityTypeEnum.DEVICE.getIndex());
        expired11.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(online10).thenReturn(expired11);
        assertThat(service.getDeviceOnlineByDriverId(1L, 7L)).isEqualTo(1L);
    }

    @Test
    void getDeviceOfflineByDriverIdCountsOfflineAndMissingDevices() {
        when(driverFacade.getById(1L, 7L)).thenReturn(driver(7L));
        when(deviceFacade.listByDriverId(1L, 7L)).thenReturn(List.of(device(10L), device(11L), device(12L)));
        EntityStateDO online10 = onlineState(10L, EntityTypeEnum.DEVICE.getIndex());
        online10.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(online10).thenReturn(null).thenReturn(null);
        assertThat(service.getDeviceOfflineByDriverId(1L, 7L)).isEqualTo(2L);
    }
}
