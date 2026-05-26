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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.exception.ImportException;
import io.github.pnoker.common.manager.biz.ImportDeviceService;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.DriverAttributeConfigManager;
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.builder.DeviceBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.DriverAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.PointAttributeConfigDO;
import io.github.pnoker.common.utils.PoiUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Driver synchronization interface implementation
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportDeviceServiceImpl implements ImportDeviceService {

    private final DeviceBuilder deviceBuilder;

    private final DeviceManager deviceManager;

    private final DriverAttributeConfigManager driverAttributeConfigManager;

    private final PointAttributeConfigManager pointAttributeConfigManager;

    @Override
    @Transactional
    public DeviceBO importDevice(DeviceBO deviceBO, List<PointBO> pointBOList,
                                 List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, Sheet sheet,
                                 int row) {
        String deviceName = PoiUtil.getCellStringValue(sheet, row, 0);
        if (StringUtils.isEmpty(deviceName)) {
            throw new ImportException("The device name in line {} of the import file is empty", row + 1);
        }

        DeviceDO entityDO = new DeviceDO();
        entityDO.setDeviceName(deviceName);
        entityDO.setDriverId(deviceBO.getDriverId());
        String deviceRemark = PoiUtil.getCellStringValue(sheet, row, 1);
        entityDO.setDeviceExt(new JsonExt());
        entityDO.setRemark(deviceRemark);
        entityDO.setTenantId(deviceBO.getTenantId());

        // Import device
        entityDO.setProfileId(deviceBO.getProfileId());
        entityDO = deviceManager.innerSave(entityDO);
        DeviceBO entityBO = deviceBuilder.buildBOByDO(entityDO);

        // Import driver attribute configuration
        importDriverAttributeConfig(entityBO, driverAttributeBOList, sheet, row);

        // Import point attribute configuration
        importPointAttributeConfig(entityBO, pointBOList, driverAttributeBOList, pointAttributeBOList, sheet, row);

        return entityBO;
    }

    /**
     * Import driver attribute configuration
     *
     * @param deviceBO              Device
     * @param driverAttributeBOList DriverAttributeBO Array
     * @param sheet                 Sheet
     * @param row                   Row Index
     */
    private void importDriverAttributeConfig(DeviceBO deviceBO, List<DriverAttributeBO> driverAttributeBOList,
                                             Sheet sheet, int row) {
        List<DriverAttributeConfigDO> entities = new ArrayList<>();
        for (int j = 0; j < driverAttributeBOList.size(); j++) {
            DriverAttributeConfigDO entityDO = new DriverAttributeConfigDO();
            DriverAttributeBO driverAttributeBO = driverAttributeBOList.get(j);
            entityDO.setAttributeId(driverAttributeBO.getId());
            entityDO.setDeviceId(deviceBO.getId());
            entityDO.setConfigValue(PoiUtil.getCellStringValue(sheet, row, 2 + j));
            entityDO.setRemark(deviceBO.getRemark());
            entityDO.setTenantId(deviceBO.getTenantId());
            entities.add(entityDO);
        }
        if (!entities.isEmpty()) {
            driverAttributeConfigManager.saveBatch(entities);
        }
    }

    /**
     * Import point attribute configuration
     *
     * @param deviceBO              Device
     * @param driverAttributeBOList DriverAttributeBO Array
     * @param pointAttributeBOList  PointAttributeBO Array
     * @param pointBOList           PointBO Array
     * @param sheet                 Sheet
     * @param row                   Row Index
     */
    private void importPointAttributeConfig(DeviceBO deviceBO, List<PointBO> pointBOList,
                                            List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, Sheet sheet,
                                            int row) {
        List<PointAttributeConfigDO> entities = new ArrayList<>();
        for (int j = 0; j < pointBOList.size(); j++) {
            for (int k = 0; k < pointAttributeBOList.size(); k++) {
                PointAttributeConfigDO entityDO = new PointAttributeConfigDO();
                PointBO pointBO = pointBOList.get(j);
                PointAttributeBO pointAttributeBO = pointAttributeBOList.get(k);
                entityDO.setAttributeId(pointAttributeBO.getId());
                entityDO.setDeviceId(deviceBO.getId());
                entityDO.setPointId(pointBO.getId());
                entityDO.setConfigValue(PoiUtil.getCellStringValue(sheet, row,
                        2 + driverAttributeBOList.size() + k * pointAttributeBOList.size() + j));
                entityDO.setRemark(deviceBO.getRemark());
                entityDO.setTenantId(deviceBO.getTenantId());
                entities.add(entityDO);
            }
        }
        if (!entities.isEmpty()) {
            pointAttributeConfigManager.saveBatch(entities);
        }
    }

}
