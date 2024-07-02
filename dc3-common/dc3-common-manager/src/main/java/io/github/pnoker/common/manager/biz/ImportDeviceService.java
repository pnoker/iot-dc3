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
