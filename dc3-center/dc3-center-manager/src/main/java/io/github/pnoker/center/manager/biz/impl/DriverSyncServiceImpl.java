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

package io.github.pnoker.center.manager.biz.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.auth.GrpcCodeQuery;
import io.github.pnoker.api.center.auth.GrpcRTenantDTO;
import io.github.pnoker.api.center.auth.TenantApiGrpc;
import io.github.pnoker.center.manager.biz.BatchService;
import io.github.pnoker.center.manager.biz.DriverSyncService;
import io.github.pnoker.center.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeBuilder;
import io.github.pnoker.center.manager.entity.builder.DriverBuilder;
import io.github.pnoker.center.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.center.manager.service.*;
import io.github.pnoker.common.constant.driver.RabbitConstant;
import io.github.pnoker.common.constant.service.AuthConstant;
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

    @GrpcClient(AuthConstant.SERVICE_NAME)
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
    public void up(DriverRegisterDTO entityDTO) {
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
                    RabbitConstant.TOPIC_EXCHANGE_REGISTER,
                    RabbitConstant.ROUTING_REGISTER_DOWN_PREFIX + entityDTO.getClient(),
                    driverSyncDownDTO
            );
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 注册驱动
     *
     * @param entityDTO DriverRegisterDTO
     */
    private DriverDTO registerDriver(DriverRegisterDTO entityDTO) {
        GrpcRTenantDTO tenantDTO = tenantApiBlockingStub.selectByCode(GrpcCodeQuery.newBuilder().setCode(entityDTO.getTenant()).build());
        if (!tenantDTO.getResult().getOk()) {
            throw new ServiceException("无效租户: {}, 错误信息: {}", entityDTO.getTenant(), tenantDTO.getResult().getMessage());
        }

        DriverDTO driverDTO = entityDTO.getDriver();
        driverDTO.setTenantId(tenantDTO.getData().getBase().getId());
        DriverBO driverBO = driverBuilder.buildBOByDTO(driverDTO);

        log.info("注册驱动: {}", JsonUtil.toJsonString(driverBO));
        DriverBO entityBO = driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
        if (ObjectUtil.isNotNull(entityBO)) {
            log.debug("驱动已注册, 执行更新: {}", JsonUtil.toJsonString(driverBO));
            driverBO.setId(entityBO.getId());
            driverService.update(driverBO);
        } else {
            log.debug("驱动未注册, 执行新增: {}", JsonUtil.toJsonString(driverBO));
            driverService.save(driverBO);
            entityBO = driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
        }

        return driverBuilder.buildDTOByBO(entityBO);
    }

    /**
     * 注册驱动属性
     *
     * @param driverRegisterDTO DriverRegisterDTO
     * @param entityDO          Driver
     */
    private void registerDriverAttribute(DriverRegisterDTO driverRegisterDTO, DriverDTO entityDO) {
        Map<String, DriverAttributeDTO> newDriverAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverRegisterDTO.getDriverAttributes()) && !driverRegisterDTO.getDriverAttributes().isEmpty()) {
            driverRegisterDTO.getDriverAttributes().forEach(driverAttribute -> newDriverAttributeMap.put(driverAttribute.getAttributeName(), driverAttribute));
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
            DriverAttributeDTO attribute = newDriverAttributeMap.get(name);
            attribute.setDriverId(entityDO.getId());
            DriverAttributeBO entityBO = driverAttributeBuilder.buildBOByDTO(attribute);
            if (oldDriverAttributeMap.containsKey(name)) {
                entityBO.setId(oldDriverAttributeMap.get(name).getId());
                log.debug("驱动属性已注册, 执行更新: {}", JsonUtil.toJsonString(entityBO));
                driverAttributeService.update(entityBO);
            } else {
                log.debug("驱动属性未注册, 执行新增: {}", JsonUtil.toJsonString(entityBO));
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
     * @param driverRegisterDTO DriverRegisterDTO
     * @param entityDO          Driver
     */
    private void registerPointAttribute(DriverRegisterDTO driverRegisterDTO, DriverDTO entityDO) {
        Map<String, PointAttributeDTO> newPointAttributeMap = new HashMap<>(8);
        if (ObjectUtil.isNotNull(driverRegisterDTO.getPointAttributes()) && !driverRegisterDTO.getPointAttributes().isEmpty()) {
            driverRegisterDTO.getPointAttributes().forEach(pointAttribute -> newPointAttributeMap.put(pointAttribute.getAttributeName(), pointAttribute));
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
                entityBO.setId(oldPointAttributeMap.get(name).getId());
                log.debug("位号属性已注册, 执行更新: {}", JsonUtil.toJsonString(entityBO));
                pointAttributeService.update(entityBO);
            } else {
                log.debug("位号属性未注册, 执行更新: {}", JsonUtil.toJsonString(entityBO));
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
