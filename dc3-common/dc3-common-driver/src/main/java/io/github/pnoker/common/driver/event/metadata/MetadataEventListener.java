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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Listens for internal metadata events and forwards supported metadata changes to the
 * custom driver service.
 *
 * @author zhangzi
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataEventListener implements ApplicationListener<MetadataEvent> {

    private final DriverCustomService driverCustomService;

    @Override
    public void onApplicationEvent(MetadataEvent metadataEvent) {
        log.debug("Metadata event listener received: id={}, type={}", metadataEvent.getId(), metadataEvent.getMetadataType());
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
        } else if (MetadataTypeEnum.DRIVER.equals(metadataType)) {
            MetadataEventDTO entityEvent = new MetadataEventDTO();
            entityEvent.setId(metadataEvent.getId());
            entityEvent.setMetadataType(MetadataTypeEnum.DRIVER);
            entityEvent.setOperateType(metadataEvent.getOperateType());
            driverCustomService.event(entityEvent);
        } else if (MetadataTypeEnum.COMMAND.equals(metadataType) || MetadataTypeEnum.EVENT.equals(metadataType)) {
            MetadataEventDTO entityEvent = new MetadataEventDTO();
            entityEvent.setId(metadataEvent.getId());
            entityEvent.setMetadataType(metadataType);
            entityEvent.setOperateType(metadataEvent.getOperateType());
            driverCustomService.event(entityEvent);
        }
    }

}
