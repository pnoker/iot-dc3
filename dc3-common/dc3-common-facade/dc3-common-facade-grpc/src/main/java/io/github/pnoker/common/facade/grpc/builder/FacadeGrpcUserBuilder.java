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

import io.github.pnoker.api.center.auth.GrpcUserDTO;
import io.github.pnoker.common.facade.entity.bo.FacadeUserBO;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * {@code GrpcUserDTO} → {@link FacadeUserBO}. {@code socialExt}/{@code identityExt} stay
 * as opaque JSON strings — keeping the wire representation untouched.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Component
public class FacadeGrpcUserBuilder {

	public FacadeUserBO toFacadeBO(GrpcUserDTO dto) {
		if (Objects.isNull(dto)) {
			return null;
		}

		FacadeUserBO bo = new FacadeUserBO();
		GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

		StringOptional.ofNullable(dto.getNickName()).ifPresent(bo::setNickName);
		StringOptional.ofNullable(dto.getUserName()).ifPresent(bo::setUserName);
		StringOptional.ofNullable(dto.getPhone()).ifPresent(bo::setPhone);
		StringOptional.ofNullable(dto.getEmail()).ifPresent(bo::setEmail);
		StringOptional.ofNullable(dto.getSocialExt()).ifPresent(bo::setSocialExt);
		StringOptional.ofNullable(dto.getIdentityExt()).ifPresent(bo::setIdentityExt);

		return bo;
	}

}
