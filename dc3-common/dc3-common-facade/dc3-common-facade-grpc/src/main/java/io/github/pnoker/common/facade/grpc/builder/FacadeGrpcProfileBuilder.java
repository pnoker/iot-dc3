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

import io.github.pnoker.api.center.manager.GrpcOffsetProfileQuery;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.api.common.GrpcProfileDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.ProfileExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.ProfileShareTypeEnum;
import io.github.pnoker.common.enums.ProfileTypeEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeProfileBO;
import io.github.pnoker.common.facade.entity.query.FacadeProfileOffsetQuery;
import io.github.pnoker.db.r2dbc.core.page.SortSpec;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.List;

/**
 * Converts between facade profile shapes and protobuf profile DTOs.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcProfileBuilder {

    /** Convert the canonical offset query to protobuf. */
    public GrpcOffsetProfileQuery toGrpcOffsetQuery(FacadeProfileOffsetQuery query) {
        GrpcOffsetProfileQuery.Builder builder = GrpcOffsetProfileQuery.newBuilder()
                .setPage(PageRequest.newBuilder().setOffset(query.offset()).setLimit(query.limit())
                        .addAllSort(query.sort().stream().map(this::toGrpcSort).toList()).build())
                .setTenantId(query.tenantId());
        StringOptional.ofNullable(query.profileName()).ifPresent(builder::setProfileName);
        StringOptional.ofNullable(query.profileCode()).ifPresent(builder::setProfileCode);
        LongOptional.ofNullable(query.groupId()).ifPresent(builder::setGroupId);
        LongOptional.ofNullable(query.labelId()).ifPresent(builder::setLabelId);
        LongOptional.ofNullable(query.deviceId()).ifPresent(builder::setDeviceId);
        Optional.ofNullable(query.profileShareFlag()).ifPresent(value -> builder.setProfileShareFlag(value.getIndex()));
        Optional.ofNullable(query.profileTypeFlag()).ifPresent(value -> builder.setProfileTypeFlag(value.getIndex()));
        Optional.ofNullable(query.enableFlag()).ifPresent(value -> builder.setEnableFlag(value.getIndex()));
        Optional.ofNullable(query.version()).ifPresent(builder::setVersion);
        return builder.build();
    }

    private io.github.pnoker.api.common.SortSpec toGrpcSort(SortSpec sort) {
        return io.github.pnoker.api.common.SortSpec.newBuilder().setField(sort.field()).setDirection(
                sort.direction() == SortSpec.Direction.ASC ? SortDirection.SORT_DIRECTION_ASC : SortDirection.SORT_DIRECTION_DESC).build();
    }

    /**
     * To facade business object.
     *
     * @param dto dto
     * @return to facade business object result
     */
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
