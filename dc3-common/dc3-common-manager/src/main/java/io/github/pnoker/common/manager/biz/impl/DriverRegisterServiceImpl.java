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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Driver synchronization interface implementation
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverRegisterServiceImpl implements DriverRegisterService {

	private final GrpcDriverBuilder grpcDriverBuilder;

	private final GrpcDriverAttributeBuilder grpcDriverAttributeBuilder;

	private final GrpcPointAttributeBuilder grpcPointAttributeBuilder;

	private final DriverService driverService;

	private final DriverAttributeService driverAttributeService;

	private final PointAttributeService pointAttributeService;

	private final TenantFacade tenantFacade;

	public DriverRegisterServiceImpl(GrpcDriverBuilder grpcDriverBuilder,
			GrpcDriverAttributeBuilder grpcDriverAttributeBuilder, GrpcPointAttributeBuilder grpcPointAttributeBuilder,
			DriverService driverService, DriverAttributeService driverAttributeService,
			PointAttributeService pointAttributeService, TenantFacade tenantFacade) {
		this.grpcDriverBuilder = grpcDriverBuilder;
		this.grpcDriverAttributeBuilder = grpcDriverAttributeBuilder;
		this.grpcPointAttributeBuilder = grpcPointAttributeBuilder;
		this.driverService = driverService;
		this.driverAttributeService = driverAttributeService;
		this.pointAttributeService = pointAttributeService;
		this.tenantFacade = tenantFacade;
	}

	@Override
	public DriverBO registerDriver(GrpcDriverRegisterDTO entityGrpc) {
		FacadeTenantBO tenant = tenantFacade.selectByCode(entityGrpc.getTenant());
		if (Objects.isNull(tenant)) {
			throw new ServiceException("Tenant[{}] information is invalid", entityGrpc.getTenant());
		}

		DriverBO driverBO = grpcDriverBuilder.buildBOByGrpcDTO(entityGrpc.getDriver());
		Objects.requireNonNull(driverBO).setTenantId(tenant.getId());
		DriverBO entityBO = driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
		if (Objects.nonNull(entityBO)) {
			log.info("The driver has been registered, perform update: {}", JsonUtil.toJsonString(driverBO));
			driverBO.setId(entityBO.getId());
			driverService.update(driverBO);
		}
		else {
			log.info("The driver is not registered, perform new addition: {}", JsonUtil.toJsonString(driverBO));
			driverService.save(driverBO);
		}

		return driverService.selectByServiceName(driverBO.getServiceName(), driverBO.getTenantId());
	}

	@Override
	public List<DriverAttributeBO> registerDriverAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
		Map<String, DriverAttributeBO> newDriverAttributeMap = entityGrpc.getDriverAttributesList()
			.stream()
			.collect(Collectors.toMap(GrpcDriverAttributeDTO::getAttributeCode,
					grpcDriverAttributeBuilder::buildBOByGrpcDTO));

		Map<String, DriverAttributeBO> oldDriverAttributeMap = driverAttributeService.selectByDriverId(entityBO.getId())
			.stream()
			.collect(Collectors.toMap(DriverAttributeBO::getAttributeCode, Function.identity()));

		for (Map.Entry<String, DriverAttributeBO> entry : newDriverAttributeMap.entrySet()) {
			String name = entry.getKey();
			DriverAttributeBO attribute = newDriverAttributeMap.get(name);
			attribute.setDriverId(entityBO.getId());
			if (oldDriverAttributeMap.containsKey(name)) {
				log.debug("The driver attributes have been registered, update is performed: {}",
						JsonUtil.toJsonString(attribute));
				attribute.setId(oldDriverAttributeMap.get(name).getId());
				driverAttributeService.update(attribute);
			}
			else {
				log.debug("The driver attributes are not registered, perform new addition: {}",
						JsonUtil.toJsonString(attribute));
				driverAttributeService.save(attribute);
			}
		}

		for (Map.Entry<String, DriverAttributeBO> entry : oldDriverAttributeMap.entrySet()) {
			String name = entry.getKey();
			if (!newDriverAttributeMap.containsKey(name)) {
				log.debug("Driver attribute is redundant, deleting: {}", oldDriverAttributeMap.get(name));
				driverAttributeService.remove(oldDriverAttributeMap.get(name).getId());
			}
		}

		return driverAttributeService.selectByDriverId(entityBO.getId());
	}

	@Override
	public List<PointAttributeBO> registerPointAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO) {
		Map<String, PointAttributeBO> newPointAttributeMap = entityGrpc.getPointAttributesList()
			.stream()
			.collect(Collectors.toMap(GrpcPointAttributeDTO::getAttributeCode,
					grpcPointAttributeBuilder::buildBOByGrpcDTO));

		Map<String, PointAttributeBO> oldPointAttributeMap = pointAttributeService.selectByDriverId(entityBO.getId())
			.stream()
			.collect(Collectors.toMap(PointAttributeBO::getAttributeCode, Function.identity()));

		for (Map.Entry<String, PointAttributeBO> entry : newPointAttributeMap.entrySet()) {
			String name = entry.getKey();
			PointAttributeBO attribute = newPointAttributeMap.get(name);
			attribute.setDriverId(entityBO.getId());
			if (oldPointAttributeMap.containsKey(name)) {
				log.debug("The point attribute has been registered, update is performed: {}",
						JsonUtil.toJsonString(attribute));
				attribute.setId(oldPointAttributeMap.get(name).getId());
				pointAttributeService.update(attribute);
			}
			else {
				log.debug("The point attribute is not registered, perform update: {}",
						JsonUtil.toJsonString(attribute));
				pointAttributeService.save(attribute);
			}
		}

		for (Map.Entry<String, PointAttributeBO> entry : oldPointAttributeMap.entrySet()) {
			String name = entry.getKey();
			if (!newPointAttributeMap.containsKey(name)) {
				log.debug("Point attribute is redundant, deleting: {}", oldPointAttributeMap.get(name));
				pointAttributeService.remove(oldPointAttributeMap.get(name).getId());
			}
		}

		return pointAttributeService.selectByDriverId(entityBO.getId());
	}

}
