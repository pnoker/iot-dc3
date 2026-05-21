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

package io.github.pnoker.common.manager.biz.impl;

import io.github.pnoker.api.common.GrpcDriverAttributeDTO;
import io.github.pnoker.api.common.GrpcPointAttributeDTO;
import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.exception.ServiceException;
import io.github.pnoker.common.facade.api.TenantFacade;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.manager.biz.DriverRegisterService;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverAttributeBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcDriverBuilder;
import io.github.pnoker.common.manager.grpc.builder.GrpcPointAttributeBuilder;
import io.github.pnoker.common.manager.service.DriverAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Driver synchronization interface implementation
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverRegisterServiceImpl implements DriverRegisterService {

    private final GrpcDriverBuilder grpcDriverBuilder;

    private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

    private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;

    private final DriverService driverService;

    private final DriverAttributeService driverAttributeService;

    private final PointAttributeService pointAttributeService;

    private final TenantFacade tenantFacade;

    @Override
    public DriverBO registerDriver(GrpcDriverRegisterDTO entityGrpc) {
        FacadeTenantBO tenant = tenantFacade.getByCode(entityGrpc.getTenant());
        if (Objects.isNull(tenant)) {
            throw new ServiceException("Tenant[{}] information is invalid", entityGrpc.getTenant());
        }

        DriverBO driverBO = grpcDriverBuilder.buildBOByGrpcDTO(entityGrpc.getDriver());
        Objects.requireNonNull(driverBO).setTenantId(tenant.getId());
        DriverBO entityBO = driverService.getByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
        if (Objects.nonNull(entityBO)) {
            log.info("The driver has been registered, perform update: {}", JsonUtil.toJsonString(driverBO));
            driverBO.setId(entityBO.getId());
            driverService.update(driverBO);
        } else {
            log.info("The driver is not registered, perform new addition: {}", JsonUtil.toJsonString(driverBO));
            driverService.add(driverBO);
        }

        return driverService.getByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
    }

    @Override
    public List<DriverAttributeBO> registerDriverAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
        Map<String, DriverAttributeBO> newDriverAttributeMap = entityGrpc.getDriverAttributesList()
                .stream()
                .collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode,
                        grpcDriverAttributeBuilder::buildBOByGrpcDTO));

        Map<String, DriverAttributeBO> oldDriverAttributeMap = driverAttributeService.listByDriverId(entityBO.getId())
                .stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .collect(Collectors.toMap(DriverAttributeBO::getAttributeCode, Function.identity()));

        // Diff into three buckets, then issue at most three round-trips (was N round-trips
        // when a driver came up with many attributes — re-registration storms hit the DB).
        List<DriverAttributeBO> toInsert = new ArrayList<>();
        List<DriverAttributeBO> toUpdate = new ArrayList<>();
        for (Map.Entry<String, DriverAttributeBO> entry : newDriverAttributeMap.entrySet()) {
            DriverAttributeBO attribute = entry.getValue();
            attribute.setDriverId(entityBO.getId());
            attribute.setTenantId(entityBO.getTenantId());
            DriverAttributeBO existing = oldDriverAttributeMap.get(entry.getKey());
            if (Objects.nonNull(existing)) {
                attribute.setId(existing.getId());
                toUpdate.add(attribute);
            } else {
                toInsert.add(attribute);
            }
        }
        Set<Long> toRemoveIds = new HashSet<>();
        for (Map.Entry<String, DriverAttributeBO> entry : oldDriverAttributeMap.entrySet()) {
            if (!newDriverAttributeMap.containsKey(entry.getKey())) {
                toRemoveIds.add(entry.getValue().getId());
            }
        }
        log.debug("Driver attribute diff for driver {}: insert={} update={} remove={}", entityBO.getId(),
                toInsert.size(), toUpdate.size(), toRemoveIds.size());
        driverAttributeService.saveBatch(toInsert);
        driverAttributeService.updateBatch(toUpdate);
        driverAttributeService.removeByIds(toRemoveIds);

        return driverAttributeService.listByDriverId(entityBO.getId()).stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .toList();
    }

    @Override
    public List<PointAttributeBO> registerPointAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
        Map<String, PointAttributeBO> newPointAttributeMap = entityGrpc.getPointAttributesList()
                .stream()
                .collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode,
                        grpcPointAttributeBuilder::buildBOByGrpcDTO));

        Map<String, PointAttributeBO> oldPointAttributeMap = pointAttributeService.listByDriverId(entityBO.getId())
                .stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .collect(Collectors.toMap(PointAttributeBO::getAttributeCode, Function.identity()));

        // See registerDriverAttribute — same three-bucket batch pattern.
        List<PointAttributeBO> pointToInsert = new ArrayList<>();
        List<PointAttributeBO> pointToUpdate = new ArrayList<>();
        for (Map.Entry<String, PointAttributeBO> entry : newPointAttributeMap.entrySet()) {
            PointAttributeBO attribute = entry.getValue();
            attribute.setDriverId(entityBO.getId());
            attribute.setTenantId(entityBO.getTenantId());
            PointAttributeBO existing = oldPointAttributeMap.get(entry.getKey());
            if (Objects.nonNull(existing)) {
                attribute.setId(existing.getId());
                pointToUpdate.add(attribute);
            } else {
                pointToInsert.add(attribute);
            }
        }
        Set<Long> pointToRemoveIds = new HashSet<>();
        for (Map.Entry<String, PointAttributeBO> entry : oldPointAttributeMap.entrySet()) {
            if (!newPointAttributeMap.containsKey(entry.getKey())) {
                pointToRemoveIds.add(entry.getValue().getId());
            }
        }
        log.debug("Point attribute diff for driver {}: insert={} update={} remove={}", entityBO.getId(),
                pointToInsert.size(), pointToUpdate.size(), pointToRemoveIds.size());
        pointAttributeService.saveBatch(pointToInsert);
        pointAttributeService.updateBatch(pointToUpdate);
        pointAttributeService.removeByIds(pointToRemoveIds);

        return pointAttributeService.listByDriverId(entityBO.getId()).stream()
                .filter(attribute -> Objects.equals(entityBO.getTenantId(), attribute.getTenantId()))
                .toList();
    }

}
