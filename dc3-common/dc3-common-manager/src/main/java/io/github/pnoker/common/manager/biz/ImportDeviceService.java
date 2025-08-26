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

package io.github.pnoker.common.manager.biz;

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

/**
 * 导入相关接口
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface ImportDeviceService {

    /**
     * 导入设备
     *
     * @param deviceBO              设备
     * @param pointBOList           位号集合
     * @param driverAttributeBOList 驱动属性配置集合
     * @param pointAttributeBOList  驱动属性配置集合
     * @param sheet                 Sheet
     * @param row                   Row Index
     */
    DeviceBO importDevice(DeviceBO deviceBO, List<PointBO> pointBOList, List<DriverAttributeBO> driverAttributeBOList, List<PointAttributeBO> pointAttributeBOList, Sheet sheet, int row);

}
