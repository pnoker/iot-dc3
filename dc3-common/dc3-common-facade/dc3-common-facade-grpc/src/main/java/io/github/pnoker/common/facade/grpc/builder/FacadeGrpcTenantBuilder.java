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

package io.github.pnoker.common.facade.grpc.builder;

import io.github.pnoker.api.center.auth.GrpcTenantDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeTenantBO;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * {@code GrpcTenantDTO} → {@link FacadeTenantBO}.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Component
public class FacadeGrpcTenantBuilder {

	public FacadeTenantBO toFacadeBO(GrpcTenantDTO dto) {
		if (Objects.isNull(dto)) {
			return null;
		}

		FacadeTenantBO bo = new FacadeTenantBO();
		GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

		StringOptional.ofNullable(dto.getTenantName()).ifPresent(bo::setTenantName);
		StringOptional.ofNullable(dto.getTenantCode()).ifPresent(bo::setTenantCode);
		Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

		return bo;
	}

}
