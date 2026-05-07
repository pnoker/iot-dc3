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

import io.github.pnoker.api.center.auth.GrpcUserLoginDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeUserLoginBO;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * {@code GrpcUserLoginDTO} → {@link FacadeUserLoginBO}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Component
public class FacadeGrpcUserLoginBuilder {

    public FacadeUserLoginBO toFacadeBO(GrpcUserLoginDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeUserLoginBO bo = new FacadeUserLoginBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getLoginName()).ifPresent(bo::setLoginName);
        LongOptional.ofNullable(dto.getUserId()).ifPresent(bo::setUserId);
        LongOptional.ofNullable(dto.getUserPasswordId()).ifPresent(bo::setUserPasswordId);
        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        return bo;
    }

}
