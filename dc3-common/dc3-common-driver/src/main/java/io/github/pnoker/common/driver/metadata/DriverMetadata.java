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

package io.github.pnoker.common.driver.metadata;


import io.github.pnoker.common.driver.entity.bo.DriverBO;
import io.github.pnoker.common.driver.entity.dto.DriverAttributeDTO;
import io.github.pnoker.common.driver.entity.dto.PointAttributeDTO;
import io.github.pnoker.common.enums.DriverStatusEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

/**
 * 驱动元数据
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@ToString
@Component
public final class DriverMetadata {

    /**
     * 驱动状态
     */
    private DriverStatusEnum driverStatus = DriverStatusEnum.OFFLINE;

    /**
     * 驱动
     */
    private DriverBO driver;

    /**
     * 设备ID集合
     */
    private Set<Long> deviceIds;

    /**
     * 驱动属性ID Map
     * <p>
     * attributeId,driverAttribute
     */
    private Map<Long, DriverAttributeDTO> driverAttributeIdMap;

    /**
     * 驱动属性名称 Map
     * <p>
     * attributeName,driverAttribute
     */
    private Map<String, DriverAttributeDTO> driverAttributeNameMap;

    /**
     * 位号属性ID Map
     * <p>
     * attributeId,pointAttribute
     */
    private Map<Long, PointAttributeDTO> pointAttributeIdMap;

    /**
     * 位号属性名称 Map
     * <p>
     * attributeName,driverAttribute
     */
    private Map<String, PointAttributeDTO> pointAttributeNameMap;
}
