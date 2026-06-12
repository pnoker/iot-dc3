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

import io.github.pnoker.api.center.auth.GrpcLocalCredentialDTO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeLocalCredentialBO;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * {@code GrpcLocalCredentialDTO} to {@link FacadeLocalCredentialBO}.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.6.12
 */
@Component
public class FacadeGrpcLocalCredentialBuilder {

    public FacadeLocalCredentialBO toFacadeBO(GrpcLocalCredentialDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeLocalCredentialBO bo = new FacadeLocalCredentialBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);
        LongOptional.ofNullable(dto.getPrincipalId()).ifPresent(bo::setPrincipalId);
        StringOptional.ofNullable(dto.getLoginName()).ifPresent(bo::setLoginName);
        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);
        return bo;
    }

}
