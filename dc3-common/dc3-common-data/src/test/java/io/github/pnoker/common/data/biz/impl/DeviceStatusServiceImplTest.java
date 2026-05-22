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
import io.github.pnoker.common.data.entity.query.DeviceQuery;
import io.github.pnoker.common.enums.EntityStatusEnum;
import io.github.pnoker.common.enums.EntityTypeFlagEnum;
import io.github.pnoker.common.facade.api.DeviceFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
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
class DeviceStatusServiceImplTest {

    @Mock
    private DeviceFacade deviceFacade;

    @Mock
    private EntityStateManager entityStateManager;

    @Mock
    private LambdaQueryChainWrapper<EntityStateDO> queryWrapper;

    @InjectMocks
    private DeviceStatusServiceImpl service;

    private FacadeDeviceBO device(Long id) {
        FacadeDeviceBO bo = new FacadeDeviceBO();
        bo.setId(id);
        return bo;
    }

    private EntityStateDO onlineState(Long entityId) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        state.setEntityId(entityId);
        state.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        state.setExpireTime(LocalDateTime.now().plusSeconds(60));
        return state;
    }

    private EntityStateDO expiredState(Long entityId) {
        EntityStateDO state = new EntityStateDO();
        state.setEntityTypeFlag((byte) EntityTypeFlagEnum.DEVICE.getIndex());
        state.setEntityId(entityId);
        state.setStateFlag((byte) EntityStatusEnum.ONLINE.getIndex());
        state.setExpireTime(LocalDateTime.now().minusSeconds(10));
        return state;
    }

    @Test
    void getStatusByPageReturnsEmptyMapForEmptyPage() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of());
        when(deviceFacade.listByPage(any())).thenReturn(page);
        assertThat(service.getStatusByPage(new DeviceQuery())).isEmpty();
    }

    @Test
    void getStatusByPageDefaultsToOfflineWhenDbRowMissing() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L)));
        when(deviceFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(null);
        assertThat(service.getStatusByPage(new DeviceQuery()))
                .containsEntry(10L, EntityStatusEnum.OFFLINE.getCode());
    }

    @Test
    void getStatusByPageReturnsOnlineFromDb() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L)));
        when(deviceFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(onlineState(10L));
        assertThat(service.getStatusByPage(new DeviceQuery()))
                .containsEntry(10L, EntityStatusEnum.ONLINE.getCode());
    }

    @Test
    void getStatusByPageReturnsOfflineWhenExpired() {
        FacadePage<FacadeDeviceBO> page = new FacadePage<>();
        page.setRecords(List.of(device(10L)));
        when(deviceFacade.listByPage(any())).thenReturn(page);
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(expiredState(10L));
        assertThat(service.getStatusByPage(new DeviceQuery()))
                .containsEntry(10L, EntityStatusEnum.OFFLINE.getCode());
    }

    @Test
    void listByProfileIdReturnsEmptyMapForEmptyDevices() {
        when(deviceFacade.listByProfileId(1L, 5L)).thenReturn(List.of());
        assertThat(service.listByProfileId(1L, 5L)).isEmpty();
    }

    @Test
    void listByProfileIdMapsAllDevicesToDbOrOfflineStatus() {
        when(deviceFacade.listByProfileId(1L, 5L)).thenReturn(List.of(device(10L), device(11L)));
        when(entityStateManager.lambdaQuery()).thenReturn(queryWrapper);
        when(queryWrapper.eq(any(), any())).thenReturn(queryWrapper);
        when(queryWrapper.one()).thenReturn(onlineState(10L)).thenReturn(null);
        assertThat(service.listByProfileId(1L, 5L))
                .containsEntry(10L, EntityStatusEnum.ONLINE.getCode())
                .containsEntry(11L, EntityStatusEnum.OFFLINE.getCode());
    }
}
