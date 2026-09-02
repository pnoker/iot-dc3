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
package io.github.pnoker.common.manager.grpc.builder;

import io.github.pnoker.api.center.manager.GrpcOffsetProfileQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.grpc.GrpcPageUtil;
import io.github.pnoker.common.manager.repository.ProfileFilter;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * MapStruct builder for profile gRPC message conversion.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class GrpcProfileBuilder {

    /** Convert the canonical offset request into a validated repository filter. */
    public ProfileFilter buildFilterByGrpcQuery(GrpcOffsetProfileQuery request) {
        var page = GrpcPageUtil.require(request.hasPage() ? request.getPage() : null);
        return new ProfileFilter(
                request.getTenantId(),
                request.getProfileName(),
                request.getProfileCode(),
                request.hasProfileShareFlag()
                        ? ProfileShareTypeEnum.ofIndex((byte) request.getProfileShareFlag())
                        : null,
                request.hasProfileTypeFlag() ? ProfileTypeEnum.ofIndex((byte) request.getProfileTypeFlag()) : null,
                request.hasEnableFlag() ? EnableFlagEnum.ofIndex((byte) request.getEnableFlag()) : null,
                request.hasGroupId() ? request.getGroupId() : null,
                request.hasLabelId() ? request.getLabelId() : null,
                request.hasVersion() ? request.getVersion() : null,
                request.hasDeviceId() ? request.getDeviceId() : null,
                page.offset(),
                page.limit(),
                page.sort());
    }

    /**
     * Convert bo to grpc transfer object.
     *
     * @param entityBO business object
     * @return converted value
     */
    public GrpcProfileDTO buildGrpcDTOByBO(ProfileBO entityBO) {
        GrpcProfileDTO.Builder builder = GrpcProfileDTO.newBuilder();
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(grpcBase);

        StringOptional.ofNullable(entityBO.getProfileName()).ifPresent(builder::setProfileName);
        StringOptional.ofNullable(entityBO.getProfileCode()).ifPresent(builder::setProfileCode);
        Optional.ofNullable(entityBO.getProfileShareFlag())
                .ifPresentOrElse(
                        value -> builder.setProfileShareFlag(value.getIndex()),
                        () -> builder.setProfileShareFlag(DefaultConstant.DEFAULT_INT));
        Optional.ofNullable(entityBO.getProfileTypeFlag())
                .ifPresentOrElse(
                        value -> builder.setProfileTypeFlag(value.getIndex()),
                        () -> builder.setProfileTypeFlag(DefaultConstant.DEFAULT_INT));
        Optional.ofNullable(entityBO.getProfileExt())
                .ifPresent(value -> builder.setProfileExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getEnableFlag())
                .ifPresentOrElse(
                        value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.DEFAULT_INT));
        StringOptional.ofNullable(entityBO.getSignature()).ifPresent(builder::setSignature);
        if (Objects.nonNull(entityBO.getVersion())) {
            builder.setVersion(entityBO.getVersion());
        }
        LongOptional.ofNullable(entityBO.getTenantId()).ifPresent(builder::setTenantId);
        return builder.build();
    }
}
