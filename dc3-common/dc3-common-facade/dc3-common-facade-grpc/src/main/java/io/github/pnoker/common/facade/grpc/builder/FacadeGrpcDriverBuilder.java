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

import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.common.GrpcDriverDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.DriverExt;
import io.github.pnoker.common.enums.DriverTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Hand-rolled conversion between facade shapes and protobuf driver types.
 * <p>
 * Driver uses {@code NULL_INT = -1} as the "not set" marker for {@code driverTypeFlag}
 * (unlike Device, which piggy-backs on {@code DEFAULT_INT = 0}).
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcDriverBuilder {

    public GrpcPageDriverQuery toGrpcPageQuery(FacadeDriverQuery query) {
        GrpcPageDriverQuery.Builder builder = GrpcPageDriverQuery.newBuilder();

        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        GrpcPage.Builder page = GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize());
        builder.setPage(page);

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getDriverName()).ifPresent(builder::setDriverName);
        StringOptional.ofNullable(query.getDriverCode()).ifPresent(builder::setDriverCode);
        StringOptional.ofNullable(query.getServiceName()).ifPresent(builder::setServiceName);
        StringOptional.ofNullable(query.getServiceHost()).ifPresent(builder::setServiceHost);
        Optional.ofNullable(query.getDriverTypeFlag())
                .ifPresentOrElse(value -> builder.setDriverTypeFlag(value.getIndex()),
                        () -> builder.setDriverTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));

        return builder.build();
    }

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
