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

package io.github.pnoker.common.driver.entity.bo;

import io.github.pnoker.common.driver.entity.dto.DriverAttributeConfigDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeConfigDTO;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import lombok.*;

import java.util.Map;
import java.util.Set;

/**
 * 设备 BO
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class DeviceBO extends BaseBO {

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备编号
     */
    private String deviceCode;

    /**
     * 驱动ID
     */
    private Long driverId;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 设备拓展信息
     */
    private DeviceExt deviceExt;

    /**
     * 使能标识
     */
    private EnableFlagEnum enableFlag;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 签名
     */
    private String signature;

    /**
     * 版本
     */
    private Integer version;

    // 附加字段

    /**
     * 模版ID集
     */
    private Set<Long> profileIds;

    /**
     * 位号ID集
     */
    private Set<Long> pointIds;

    /**
     * 驱动配置
     * <p>
     * attributeId,attributeConfig
     */
    private Map<Long, DriverAttributeConfigDTO> driverAttributeConfigIdMap;

    /**
     * 位号配置
     * pointId(attributeId,attributeConfig)
     */
    private Map<Long, Map<Long, PointAttributeConfigDTO>> pointAttributeConfigIdMap;
}
