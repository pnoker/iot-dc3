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

import io.github.pnoker.api.center.manager.GrpcPageProfileQuery;
import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.query.ProfileQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * GrpcProfile Builder.
 *
 * @author pnoker
 * @version 2026.5.14
 * @since 2016.10.1
 */
@Component
public class GrpcProfileBuilder {

    public ProfileQuery buildQueryByGrpcQuery(GrpcPageProfileQuery entityGrpc) {
        ProfileQuery.ProfileQueryBuilder builder = ProfileQuery.builder();
        Pages pages = GrpcBuilderUtil.buildPagesByGrpcPage(entityGrpc.getPage());
        builder.page(pages);
        LongOptional.ofNullable(entityGrpc.getTenantId()).ifPresent(builder::tenantId);
        StringOptional.ofNullable(entityGrpc.getProfileName()).ifPresent(builder::profileName);
        StringOptional.ofNullable(entityGrpc.getProfileCode()).ifPresent(builder::profileCode);
        if (entityGrpc.getProfileShareFlag() != DefaultConstant.NULL_INT) {
            builder.profileShareFlag(ProfileShareTypeEnum.ofIndex((byte) entityGrpc.getProfileShareFlag()));
        }
        if (entityGrpc.getProfileTypeFlag() != DefaultConstant.NULL_INT) {
            builder.profileTypeFlag(ProfileTypeEnum.ofIndex((byte) entityGrpc.getProfileTypeFlag()));
        }
        if (entityGrpc.getEnableFlag() != DefaultConstant.NULL_INT) {
            builder.enableFlag(EnableFlagEnum.ofIndex((byte) entityGrpc.getEnableFlag()));
        }
        if (entityGrpc.getVersion() != DefaultConstant.NULL_INT) {
            builder.version(entityGrpc.getVersion());
        }
        LongOptional.ofNullable(entityGrpc.getDeviceId()).ifPresent(builder::deviceId);
        return builder.build();
    }

    public GrpcProfileDTO buildGrpcDTOByBO(ProfileBO entityBO) {
        GrpcProfileDTO.Builder builder = GrpcProfileDTO.newBuilder();
        GrpcBase grpcBase = GrpcBuilderUtil.buildGrpcBaseByBO(entityBO);
        builder.setBase(grpcBase);

        StringOptional.ofNullable(entityBO.getProfileName()).ifPresent(builder::setProfileName);
        StringOptional.ofNullable(entityBO.getProfileCode()).ifPresent(builder::setProfileCode);
        Optional.ofNullable(entityBO.getProfileShareFlag())
                .ifPresentOrElse(value -> builder.setProfileShareFlag(value.getIndex()),
                        () -> builder.setProfileShareFlag(DefaultConstant.DEFAULT_INT));
        Optional.ofNullable(entityBO.getProfileTypeFlag())
                .ifPresentOrElse(value -> builder.setProfileTypeFlag(value.getIndex()),
                        () -> builder.setProfileTypeFlag(DefaultConstant.DEFAULT_INT));
        Optional.ofNullable(entityBO.getProfileExt())
                .ifPresent(value -> builder.setProfileExt(JsonUtil.toJsonString(value)));
        Optional.ofNullable(entityBO.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.DEFAULT_INT));
        StringOptional.ofNullable(entityBO.getSignature()).ifPresent(builder::setSignature);
        if (Objects.nonNull(entityBO.getVersion())) {
            builder.setVersion(entityBO.getVersion());
        }
        LongOptional.ofNullable(entityBO.getTenantId()).ifPresent(builder::setTenantId);
        return builder.build();
    }

    public GrpcPage buildGrpcPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page<ProfileBO> page) {
        return GrpcPage.newBuilder()
                .setCurrent(page.getCurrent())
                .setSize(page.getSize())
                .setPages(page.getPages())
                .setTotal(page.getTotal())
                .build();
    }

}
