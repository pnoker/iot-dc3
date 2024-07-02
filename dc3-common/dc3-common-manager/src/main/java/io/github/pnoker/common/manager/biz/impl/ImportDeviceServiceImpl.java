/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.manager.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.exception.ImportException;
import io.github.pnoker.common.manager.biz.ImportDeviceService;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.entity.bo.*;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.ProfileBindService;
import io.github.pnoker.common.utils.PoiUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 驱动同步相关接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ImportDeviceServiceImpl implements ImportDeviceService {

    @Resource
    private DeviceBuilder deviceBuilder;

    @Resource
    private DeviceManager deviceManager;
    @Resource
    private ProfileBindService profileBindService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    @Override
    @Transactional
    public DeviceBO importDevice(DeviceBO deviceBO, List<PointBO> pointBOList, List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, Sheet sheet, int row) {
        String deviceName = PoiUtil.getCellStringValue(sheet, row, 0);
        if (CharSequenceUtil.isEmpty(deviceName)) {
            throw new ImportException("The device name in line {} of the import file is empty", row + 1);
        }

        DeviceDO entityDO = new DeviceDO();
        entityDO.setDeviceName(deviceName);
        entityDO.setDriverId(deviceBO.getDriverId());
        String deviceRemark = PoiUtil.getCellStringValue(sheet, row, 1);
        entityDO.setDeviceExt(new JsonExt());
        entityDO.setRemark(deviceRemark);
        entityDO.setTenantId(deviceBO.getTenantId());

        // 导入设备
        entityDO = deviceManager.innerSave(entityDO);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);

        //导入设备模版绑定配置
        importProfileBind(entityBO, deviceBO.getProfileIds());

        // 导入驱动属性配置
        importDriverAttributeConfig(entityBO, driverAttributeBOList, sheet, row);

        // 导入位号属性配置
        importPointAttributeConfig(entityBO, pointBOList, driverAttributeBOList, pointAttributeBOList, sheet, row);

        return entityBO;
    }

    /**
     * 导入设备模版绑定配置
     *
     * @param deviceBO   Device
     * @param profileIds 模版ID集合
     */
    private void importProfileBind(DeviceBO deviceBO, List<Long> profileIds) {
        if (CollUtil.isEmpty(profileIds)) {
            return;
        }

        profileIds.forEach(profileId -> {
            try {
                ProfileBindBO entityBO = new ProfileBindBO();
                entityBO.setProfileId(profileId);
                entityBO.setDeviceId(deviceBO.getId());
                entityBO.setTenantId(deviceBO.getTenantId());
                profileBindService.save(entityBO);
            } catch (Exception ignored) {
                // nothing to do
            }
        });

    }

    /**
     * 导入驱动属性配置
     *
     * @param deviceBO              Device
     * @param driverAttributeBOList DriverAttributeBO Array
     * @param sheet                 Sheet
     * @param row                   Row Index
     */
    private void importDriverAttributeConfig(DeviceBO deviceBO, List<DriverAttributeBO> driverAttributeBOList, Sheet sheet, int row) {
        for (int j = 0; j < driverAttributeBOList.size(); j++) {
            DriverAttributeConfigBO entityBO = new DriverAttributeConfigBO();
            DriverAttributeBO driverAttributeBO = driverAttributeBOList.get(j);
            entityBO.setDriverAttributeId(driverAttributeBO.getId());
            entityBO.setDeviceId(deviceBO.getId());
            String attributeValue = PoiUtil.getCellStringValue(sheet, row, 2 + j);
            entityBO.setConfigValue(attributeValue);
            entityBO.setRemark(deviceBO.getRemark());
            entityBO.setTenantId(deviceBO.getTenantId());
            driverAttributeConfigService.innerSave(entityBO);
        }
    }

    /**
     * 导入位号属性配置
     *
     * @param deviceBO              Device
     * @param driverAttributeBOList DriverAttributeBO Array
     * @param pointAttributeBOList  PointAttributeBO Array
     * @param pointBOList           PointBO Array
     * @param sheet                 Sheet
     * @param row                   Row Index
     */
    private void importPointAttributeConfig(DeviceBO deviceBO, List<PointBO> pointBOList, List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, Sheet sheet, int row) {
        for (int j = 0; j < pointBOList.size(); j++) {
            for (int k = 0; k < pointAttributeBOList.size(); k++) {
                PointAttributeConfigBO entityBO = new PointAttributeConfigBO();
                PointBO pointBO = pointBOList.get(j);
                PointAttributeBO pointAttributeBO = pointAttributeBOList.get(k);
                entityBO.setPointAttributeId(pointAttributeBO.getId());
                entityBO.setDeviceId(deviceBO.getId());
                entityBO.setPointId(pointBO.getId());
                String attributeValue = PoiUtil.getCellStringValue(sheet, row, 2 + driverAttributeBOList.size() + k * pointAttributeBOList.size() + j);
                entityBO.setConfigValue(attributeValue);
                entityBO.setRemark(deviceBO.getRemark());
                entityBO.setTenantId(deviceBO.getTenantId());
                pointAttributeConfigService.innerSave(entityBO);
            }
        }
    }

}
