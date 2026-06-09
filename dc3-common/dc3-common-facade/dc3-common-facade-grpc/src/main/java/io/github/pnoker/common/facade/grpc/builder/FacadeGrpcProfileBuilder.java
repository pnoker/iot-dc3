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

import io.github.pnoker.api.center.manager.GrpcPageProfileQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Converts between facade profile shapes and protobuf profile DTOs.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcProfileBuilder {

    public GrpcPageProfileQuery toGrpcPageQuery(FacadeProfileQuery query) {
        GrpcPageProfileQuery.Builder builder = GrpcPageProfileQuery.newBuilder();
        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        builder.setPage(GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize()));

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getProfileName()).ifPresent(builder::setProfileName);
        StringOptional.ofNullable(query.getProfileCode()).ifPresent(builder::setProfileCode);
        Optional.ofNullable(query.getProfileShareFlag())
                .ifPresentOrElse(value -> builder.setProfileShareFlag(value.getIndex()),
                        () -> builder.setProfileShareFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getProfileTypeFlag())
                .ifPresentOrElse(value -> builder.setProfileTypeFlag(value.getIndex()),
                        () -> builder.setProfileTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getVersion())
                .ifPresentOrElse(builder::setVersion, () -> builder.setVersion(DefaultConstant.NULL_INT));
        LongOptional.ofNullable(query.getDeviceId()).ifPresent(builder::setDeviceId);
        return builder.build();
    }

    public FacadeProfileBO toFacadeBO(GrpcProfileDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }
        FacadeProfileBO bo = new FacadeProfileBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getProfileName()).ifPresent(bo::setProfileName);
        StringOptional.ofNullable(dto.getProfileCode()).ifPresent(bo::setProfileCode);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);
        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }
        Optional.ofNullable(ProfileShareTypeEnum.ofIndex((byte) dto.getProfileShareFlag()))
                .ifPresent(bo::setProfileShareFlag);
        Optional.ofNullable(ProfileTypeEnum.ofIndex((byte) dto.getProfileTypeFlag()))
                .ifPresent(bo::setProfileTypeFlag);
        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);
        StringOptional.ofNullable(dto.getProfileExt())
                .ifPresent(value -> bo.setProfileExt(JsonUtil.parseObject(value, ProfileExt.class)));
        return bo;
    }

}
