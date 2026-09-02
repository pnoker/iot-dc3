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

import io.github.pnoker.api.center.manager.GrpcOffsetDriverQuery;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverOffsetQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Hand-rolled conversion between facade shapes and protobuf driver types.
 * <p>
 * Driver uses {@code NULL_INT = -1} as the "not set" marker for {@code driverTypeFlag}
 * (unlike Device, which piggy-backs on {@code DEFAULT_INT = 0}).
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcDriverBuilder {

    public GrpcOffsetDriverQuery toGrpcOffsetQuery(FacadeDriverOffsetQuery query) {
        PageRequest.Builder page =
                PageRequest.newBuilder().setOffset(query.offset()).setLimit(query.limit());
        if (query.sort() != null)
            query.sort()
                    .forEach(spec -> page.addSort(io.github.pnoker.api.common.SortSpec.newBuilder()
                            .setField(spec.field())
                            .setDirection(
                                    spec.direction() == io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.DESC
                                            ? SortDirection.SORT_DIRECTION_DESC
                                            : SortDirection.SORT_DIRECTION_ASC)
                            .build()));
        GrpcOffsetDriverQuery.Builder builder =
                GrpcOffsetDriverQuery.newBuilder().setPage(page).setTenantId(query.tenantId());
        Optional.ofNullable(query.driverName()).ifPresent(builder::setDriverName);
        Optional.ofNullable(query.driverCode()).ifPresent(builder::setDriverCode);
        Optional.ofNullable(query.serviceName()).ifPresent(builder::setServiceName);
        Optional.ofNullable(query.serviceHost()).ifPresent(builder::setServiceHost);
        Optional.ofNullable(query.driverTypeFlag()).ifPresent(value -> builder.setDriverTypeFlag(value.getIndex()));
        Optional.ofNullable(query.enableFlag()).ifPresent(value -> builder.setEnableFlag(value.getIndex()));
        Optional.ofNullable(query.version()).ifPresent(builder::setVersion);
        Optional.ofNullable(query.groupId()).ifPresent(builder::setGroupId);
        Optional.ofNullable(query.labelId()).ifPresent(builder::setLabelId);
        return builder.build();
    }

    /**
     * To facade business object.
     *
     * @param dto dto
     * @return to facade business object result
     */
    public FacadeDriverBO toFacadeBO(GrpcDriverDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeDriverBO bo = new FacadeDriverBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getDriverName()).ifPresent(bo::setDriverName);
        StringOptional.ofNullable(dto.getDriverCode()).ifPresent(bo::setDriverCode);
        StringOptional.ofNullable(dto.getServiceName()).ifPresent(bo::setServiceName);
        StringOptional.ofNullable(dto.getServiceHost()).ifPresent(bo::setServiceHost);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);

        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }

        int driverType = dto.getDriverTypeFlag();
        if (driverType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(DriverTypeEnum.ofIndex((byte) driverType)).ifPresent(bo::setDriverTypeFlag);
        }

        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getDriverExt())
                .ifPresent(value -> bo.setDriverExt(JsonUtil.parseObject(value, DriverExt.class)));

        return bo;
    }
}
