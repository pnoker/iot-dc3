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

package io.github.pnoker.common.driver.event.metadata;

import io.github.pnoker.common.driver.service.DriverCustomService;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 元数据事件 Listener
 *
 * @author zhangzi
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Component
public class MetadataEventListener implements ApplicationListener<MetadataEvent> {

    private final DriverCustomService driverCustomService;

    public MetadataEventListener(DriverCustomService driverCustomService) {
        this.driverCustomService = driverCustomService;
    }

    @Override
    public void onApplicationEvent(MetadataEvent metadataEvent) {
        log.info("Metadata event listener received: {}", JsonUtil.toJsonString(metadataEvent));
        MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
        if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
            MetadataEventDTO entityEvent = new MetadataEventDTO();
            entityEvent.setId(metadataEvent.getId());
            entityEvent.setMetadataType(MetadataTypeEnum.DEVICE);
            entityEvent.setOperateType(metadataEvent.getOperateType());
            driverCustomService.event(entityEvent);
        } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
            MetadataEventDTO entityEvent = new MetadataEventDTO();
            entityEvent.setId(metadataEvent.getId());
            entityEvent.setMetadataType(MetadataTypeEnum.POINT);
            entityEvent.setOperateType(metadataEvent.getOperateType());
            driverCustomService.event(entityEvent);
        }
    }
}
