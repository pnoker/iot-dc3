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

import io.github.pnoker.api.center.manager.GrpcOffsetPointQuery;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.api.common.SortSpec;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointOffsetQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Hand-rolled conversion between facade shapes and protobuf point types.
 * <p>
 * {@code baseValue} / {@code multiple} are {@link BigDecimal} on the Java side but
 * {@code double} on the wire — precision loss is inherited from the existing proto
 * contract, not introduced here.
 * <p>
 * Uses {@link DefaultConstant#NULL_INT NULL_INT} as "not set" for {@code pointTypeFlag} /
 * {@code rwFlag} / {@code profileId}, matching {@code GrpcPointBuilder}.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcPointBuilder {

    /** Convert the canonical offset query to its protobuf representation. */
    public GrpcOffsetPointQuery toGrpcOffsetQuery(FacadePointOffsetQuery query) {
        GrpcOffsetPointQuery.Builder builder = GrpcOffsetPointQuery.newBuilder()
                .setPage(PageRequest.newBuilder()
                        .setOffset(query.offset())
                        .setLimit(query.limit())
                        .addAllSort(query.sort().stream().map(this::toGrpcSort).toList())
                        .build())
                .setTenantId(query.tenantId());
        StringOptional.ofNullable(query.pointName()).ifPresent(builder::setPointName);
        StringOptional.ofNullable(query.pointCode()).ifPresent(builder::setPointCode);
        LongOptional.ofNullable(query.profileId()).ifPresent(builder::setProfileId);
        LongOptional.ofNullable(query.groupId()).ifPresent(builder::setGroupId);
        LongOptional.ofNullable(query.labelId()).ifPresent(builder::setLabelId);
        LongOptional.ofNullable(query.deviceId()).ifPresent(builder::setDeviceId);
        Optional.ofNullable(query.pointTypeFlag()).ifPresent(value -> builder.setPointTypeFlag(value.getIndex()));
        Optional.ofNullable(query.rwFlag()).ifPresent(value -> builder.setRwFlag(value.getIndex()));
        Optional.ofNullable(query.enableFlag()).ifPresent(value -> builder.setEnableFlag(value.getIndex()));
        Optional.ofNullable(query.version()).ifPresent(builder::setVersion);
        return builder.build();
    }

    private SortSpec toGrpcSort(io.github.pnoker.db.r2dbc.core.page.SortSpec sort) {
        return SortSpec.newBuilder()
                .setField(sort.field())
                .setDirection(
                        sort.direction() == io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.ASC
                                ? SortDirection.SORT_DIRECTION_ASC
                                : SortDirection.SORT_DIRECTION_DESC)
                .build();
    }

    /**
     * To facade business object.
     *
     * @param dto dto
     * @return to facade business object result
     */
    public FacadePointBO toFacadeBO(GrpcPointDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadePointBO bo = new FacadePointBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getPointName()).ifPresent(bo::setPointName);
        StringOptional.ofNullable(dto.getPointCode()).ifPresent(bo::setPointCode);
        StringOptional.ofNullable(dto.getUnit()).ifPresent(bo::setUnit);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);
        LongOptional.ofNullable(dto.getProfileId()).ifPresent(bo::setProfileId);

        bo.setBaseValue(BigDecimal.valueOf(dto.getBaseValue()));
        bo.setMultiple(BigDecimal.valueOf(dto.getMultiple()));
        bo.setValueDecimal((byte) dto.getValueDecimal());

        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }

        int pointType = dto.getPointTypeFlag();
        if (pointType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(PointTypeEnum.ofIndex((byte) pointType)).ifPresent(bo::setPointTypeFlag);
        }

        int rw = dto.getRwFlag();
        if (rw != DefaultConstant.NULL_INT) {
            Optional.ofNullable(RwTypeEnum.ofIndex((byte) rw)).ifPresent(bo::setRwFlag);
        }

        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getPointExt())
                .ifPresent(value -> bo.setPointExt(JsonUtil.parseObject(value, PointExt.class)));

        return bo;
    }
}
