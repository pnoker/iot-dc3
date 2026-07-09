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

package io.github.pnoker.common.manager.event.metadata;

import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.entity.dto.MetadataEventDTO;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Objects;

/**
 * Event listener that processes metadata change events.
 *
 * @author zhangzi
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetadataEventListener {

    private final DriverService driverService;

    private final RabbitTemplate rabbitTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onApplicationEvent(MetadataEvent metadataEvent) {
        log.debug("Metadata event listener received: id={}, type={}", metadataEvent.getId(), metadataEvent.getMetadataType());
        try {
            Long id = metadataEvent.getId();
            MetadataTypeEnum metadataType = metadataEvent.getMetadataType();
            MetadataEventDTO entityDTO = new MetadataEventDTO(id, metadataType, metadataEvent.getOperateType());
            if (CollectionUtils.isNotEmpty(metadataEvent.getTargetServices())) {
                metadataEvent.getTargetServices().forEach(service -> notifyDriver(service, entityDTO));
                return;
            }

            if (MetadataTypeEnum.DEVICE.equals(metadataType)) {
                DriverBO entityBO = driverService.getByDeviceId(id, null);
                if (Objects.nonNull(entityBO)) {
                    notifyDriver(entityBO.getServiceName(), entityDTO);
                }
            } else if (MetadataTypeEnum.POINT.equals(metadataType)) {
                List<DriverBO> entityBOList = driverService.listByPointId(id, null);
                if (CollectionUtils.isNotEmpty(entityBOList)) {
                    entityBOList.forEach(entityBO -> notifyDriver(entityBO.getServiceName(), entityDTO));
                }
            } else if (MetadataTypeEnum.DRIVER.equals(metadataType)) {
                DriverBO entityBO = driverService.getById(id);
                if (Objects.nonNull(entityBO)) {
                    notifyDriver(entityBO.getServiceName(), entityDTO);
                }
            }
        } catch (Exception e) {
            log.error("Metadata event listener failed, event={}", JsonUtil.toJsonString(metadataEvent), e);
        }
    }

    /**
     * @param service
     * @param entityDTO DriverTransferMetadataDTO
     */
    private void notifyDriver(String service, MetadataEventDTO entityDTO) {
        if (Objects.isNull(service) || service.isBlank()) {
            return;
        }
        log.debug("Notify driver[{}]: id={}, type={}", service, entityDTO.getId(), entityDTO.getMetadataType());
        rabbitTemplate.convertAndSend(RabbitConstant.TOPIC_EXCHANGE_METADATA,
                RabbitConstant.ROUTING_DRIVER_METADATA_PREFIX + service, entityDTO);
    }

}
