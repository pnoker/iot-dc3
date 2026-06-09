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

import io.github.pnoker.api.center.manager.GrpcPagePointQuery;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.PointTypeEnum;
import io.github.pnoker.common.enums.RwTypeEnum;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

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
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcPointBuilder {

    public GrpcPagePointQuery toGrpcPageQuery(FacadePointQuery query) {
        GrpcPagePointQuery.Builder builder = GrpcPagePointQuery.newBuilder();

        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        GrpcPage.Builder page = GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize());
        builder.setPage(page);

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getPointName()).ifPresent(builder::setPointName);
        StringOptional.ofNullable(query.getPointCode()).ifPresent(builder::setPointCode);
        LongOptional.ofNullable(query.getDeviceId()).ifPresent(builder::setDeviceId);

        Optional.ofNullable(query.getPointTypeFlag())
                .ifPresentOrElse(value -> builder.setPointTypeFlag(value.getIndex()),
                        () -> builder.setPointTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getRwFlag())
                .ifPresentOrElse(value -> builder.setRwFlag(value.getIndex()),
                        () -> builder.setRwFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getProfileId())
                .ifPresentOrElse(builder::setProfileId, () -> builder.setProfileId(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));

        return builder.build();
    }

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
