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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.center.manager.entity.builder.DriverBuilder;
import io.github.pnoker.center.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.AuthServiceConstant;
import io.github.pnoker.common.entity.dto.*;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.ServiceException;
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

    @Resource
    private DriverBuilder driverBuilder;
    @Resource
    private DriverAttributeBuilder driverAttributeBuilder;
    @Resource
    private PointAttributeBuilder pointAttributeBuilder;

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
            DriverDTO entityDO = registerDriver(entityDTO);
            registerDriverAttribute(entityDTO, entityDO);
            registerPointAttribute(entityDTO, entityDO);
            DriverMetadataDTO driverMetadataDTO = batchService.batchDriverMetadata(entityDO.getServiceName(), entityDO.getTenantId());

            DriverSyncDownDTO driverSyncDownDTO = DriverSyncDownDTO.builder().content(JsonUtil.toJsonString(driverMetadataDTO)).build();

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
    private DriverDTO registerDriver(DriverSyncUpDTO entityDTO) {
        // check tenant
        GrpcRTenantDTO rTenantDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(entityDTO.getTenant()).build());
        if (!rTenantDTO.getResult().getOk()) {
            throw new ServiceException("Invalid {}, {}", entityDTO.getTenant(), rTenantDTO.getResult().getMessage());
        }

        // register driver
        DriverDTO entityDO = entityDTO.getDriver();
        entityDO.setTenantId(rTenantDTO.getData().getBase().getId());
        DriverBO entityBO = driverBuilder.buildBOByDTO(entityDO);
        log.info("Register driver {}", entityDO);
        try {
            DriverBO byServiceName = driverService.selectByServiceName(entityDO.getServiceName(), entityDO.getTenantId(), true);
            log.debug("Driver already registered, updating {} ", entityDO);
            entityDO.setId(byServiceName.getId());
            driverService.update(entityBO);
        } catch (NotFoundException notFoundException1) {
            log.debug("Driver does not registered, adding {} ", entityDO);
            driverService.save(entityBO);
        }
        entityBO = driverService.selectById(entityDO.getId());
        return driverBuilder.buildDTOByBO(entityBO);
    }

    /**
     * 注册驱动属性
     *
     * @param driverSyncUpDTO DriverSyncUpDTO
     * @param entityDO        Driver
     */
    private void registerDriverAttribute(DriverSyncUpDTO driverSyncUpDTO, DriverDTO entityDO) {
        Map<String, DriverAttributeDTO> newDriverAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverSyncUpDTO.getDriverAttributes()) && !driverSyncUpDTO.getDriverAttributes().isEmpty()) {
            driverSyncUpDTO.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        }

        Map<String, DriverAttributeBO> oldDriverAttributeMap = new HashMap<>(8);
        try {
            List<DriverAttributeBO> byDriverId = driverAttributeService.selectByDriverId(entityDO.getId());
            byDriverId.forEach(driverAttribute -> oldDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, DriverAttributeDTO> entry : newDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            DriverAttributeDTO info = newDriverAttributeMap.get(name);
            info.setDriverId(entityDO.getId());
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByDTO(info);
            if (oldDriverAttributeMap.containsKey(name)) {
                info.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("Driver attribute registered, updating: {}", info);
                driverAttributeService.update(entityBO);
            } else {
                log.debug("Driver attribute does not registered, adding: {}", info);
                driverAttributeService.save(entityBO);
            }
        }

        for (Map.Entry<String, DriverAttributeBO> entry : oldDriverAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newDriverAttributeMap.containsKey(name)) {
                try {
                    driverAttributeConfigService.selectByAttributeId(oldDriverAttributeMap.get(name).getId());
                    throw new ServiceException("The driver attribute(" + name + ") used by driver attribute config and cannot be deleted");
                } catch (NotFoundException notFoundException) {
                    log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
                    driverAttributeService.remove(oldDriverAttributeMap.get(name).getId());
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
    private void registerPointAttribute(DriverSyncUpDTO driverSyncUpDTO, DriverDTO entityDO) {
        Map<String, PointAttributeDTO> newPointAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverSyncUpDTO.getPointAttributes()) && !driverSyncUpDTO.getPointAttributes().isEmpty()) {
            driverSyncUpDTO.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        }

        Map<String, PointAttributeBO> oldPointAttributeMap = new HashMap<>(8);
        try {
            List<PointAttributeBO> byDriverId = pointAttributeService.selectByDriverId(entityDO.getId(), true);
            byDriverId.forEach(pointAttribute -> oldPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
        } catch (NotFoundException ignored) {
            // nothing to do
        }

        for (Map.Entry<String, PointAttributeDTO> entry : newPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            PointAttributeDTO attribute = newPointAttributeMap.get(name);
            attribute.setDriverId(entityDO.getId());
            PointAttributeBO entityBO = pointAttributeBuilder.buildBOByDTO(attribute);
            if (oldPointAttributeMap.containsKey(name)) {
                attribute.setId(oldPointAttributeMap.get(name).getId());
                log.debug("Point attribute registered, updating: {}", attribute);
                pointAttributeService.update(entityBO);
            } else {
                log.debug("Point attribute registered, adding: {}", attribute);
                pointAttributeService.save(entityBO);
            }
        }

        for (Map.Entry<String, PointAttributeBO> entry : oldPointAttributeMap.entrySet()) {
            String name = entry.getKey();
            if (!newPointAttributeMap.containsKey(name)) {
                try {
                    pointAttributeConfigService.selectByAttributeId(oldPointAttributeMap.get(name).getId());
                    throw new ServiceException("The point attribute(" + name + ") used by point attribute config and cannot be deleted");
                } catch (NotFoundException notFoundException1) {
                    log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
                    pointAttributeService.remove(oldPointAttributeMap.get(name).getId());
                }
            }
        }
    }

}
