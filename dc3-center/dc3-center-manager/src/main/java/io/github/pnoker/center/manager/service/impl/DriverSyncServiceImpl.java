/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.CodeQuery;
import io.github.pnoker.api.center.auth.RTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.dto.DriverSyncDownDTO;
import io.github.pnoker.common.dto.DriverSyncUpDTO;
import io.github.pnoker.common.entity.driver.DriverMetadata;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.model.DriverAttribute;
import io.github.pnoker.common.model.DriverDO;
import io.github.pnoker.common.model.PointAttribute;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 驱动同步相关接口实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverSyncServiceImpl implements DriverSyncService {

    @GrpcClient(AuthServiceConstant.SERVICE_NAME)
    private TenantApiGrpc.TenantApiBlockingStub tenantApiBlockingStub;

    @Resource
    private BatchService batchService;

    @Resource
    private DriverService driverService;
    @Resource
    private DriverAttributeService driverAttributeService;
    @Resource
    private DriverAttributeConfigService driverAttributeConfigService;
    @Resource
    private PointAttributeService pointAttributeService;
    @Resource
    private PointAttributeConfigService pointAttributeConfigService;

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Override
    public void up(DriverSyncUpDTO entityDTO) {
        if (ObjectUtil.isNull(entityDTO) || ObjectUtil.isNull(entityDTO.getDriver())) {
            return;
        }

        try {
            DriverDO entityDO = registerDriver(entityDTO);
            registerDriverAttribute(entityDTO, entityDO);
            registerPointAttribute(entityDTO, entityDO);
            DriverMetadata driverMetadata = batchService.batchDriverMetadata(entityDO.getServiceName(), entityDO.getTenantId());

            DriverSyncDownDTO driverSyncDownDTO = new DriverSyncDownDTO(JsonUtil.toJsonString(driverMetadata));

            rabbitTemplate.convertAndSend(
                    RabbitConstant.TOPIC_EXCHANGE_SYNC,
                    RabbitConstant.ROUTING_SYNC_DOWN_PREFIX + entityDTO.getClient(),
                    driverSyncDownDTO
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 注册驱动
     *
     * @param entityDTO DriverSyncUpDTO
     */
    private DriverDO registerDriver(DriverSyncUpDTO entityDTO) {
        // check tenant
        RTenantDTO rTenantDTO = tenantApiBlockingStub.selectByCode(CodeQuery.newBuilder().setCode(entityDTO.getTenant()).build());
        if (!rTenantDTO.getResult().getOk()) {
            throw new ServiceException("Invalid {}, {}", entityDTO.getTenant(), rTenantDTO.getResult().getMessage());
        }

        // register driver
        DriverDO entityDO = entityDTO.getDriver();
        entityDO.setTenantId(rTenantDTO.getData().getBase().getId());
        log.info("Register driver {}", entityDO);
        try {
            DriverDO byServiceName = driverService.selectByServiceName(entityDO.getServiceName(), entityDO.getTenantId(), true);
            log.debug("Driver already registered, updating {} ", entityDO);
            entityDO.setId(byServiceName.getId());
            driverService.update(entityDO);
        } catch (NotFoundException notFoundException1) {
            log.debug("Driver does not registered, adding {} ", entityDO);
            driverService.add(entityDO);
        }
        return driverService.selectById(entityDO.getId());
    }

    /**
     * 注册驱动属性
     *
     * @param driverSyncUpDTO DriverSyncUpDTO
     * @param entityDO        Driver
     */
    private void registerDriverAttribute(DriverSyncUpDTO driverSyncUpDTO, DriverDO entityDO) {
        Map<String, DriverAttribute> newDriverAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverSyncUpDTO.getDriverAttributes()) && !driverSyncUpDTO.getDriverAttributes().isEmpty()) {
            driverSyncUpDTO.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        }

        Map<String, DriverAttribute> oldDriverAttributeMap = new HashMap<>(8);
        try {
            List<DriverAttribute> byDriverId = driverAttributeService.selectByDriverId(entityDO.getId(), true);
            byDriverId.forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, DriverAttribute> entry : newDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            DriverAttribute info = newDriverAttributeMap.get(name);
            info.setDriverId(entityDO.getId());
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(info);
            } else {
                log.debug("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.add(info);
            }
        }

        for (Map.Entry<String, DriverAttribute> entry : oldDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverAttributeConfigService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver attribute config and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                    driverAttributeService.delete(oldDriverAttributeMap.get(name).getId());
                }
            }
        }
    }

    /**
     * 注册位号属性
     *
     * @param driverSyncUpDTO DriverSyncUpDTO
     * @param entityDO        Driver
     */
    private void registerPointAttribute(DriverSyncUpDTO driverSyncUpDTO, DriverDO entityDO) {
        Map<String, PointAttribute> newPointAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverSyncUpDTO.getPointAttributes()) && !driverSyncUpDTO.getPointAttributes().isEmpty()) {
            driverSyncUpDTO.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        }

        Map<String, PointAttribute> oldPointAttributeMap = new HashMap<>(8);
        try {
            List<PointAttribute> byDriverId = pointAttributeService.selectByDriverId(entityDO.getId(), true);
            byDriverId.forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, PointAttribute> entry : newPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            PointAttribute attribute = newPointAttributeMap.get(name);
            attribute.setDriverId(entityDO.getId());
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                log.debug("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(attribute);
            } else {
                log.debug("Point attribute registered, adding: {}", attribute);
                pointAttributeService.add(attribute);
            }
        }

        for (Map.Entry<String, PointAttribute> entry : oldPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointAttributeConfigService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point attribute config and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.delete(oldPointAttributeMap.get(name).getId());
                }
            }
        }
    }

}
