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

import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Converts between {@code dc3-common-facade-api} shapes and the protobuf types generated
 * from {@code api/center/manager/manager_device.proto}.
 * <p>
 * Hand-rolled rather than MapStruct because protobuf builders expose dozens of generated
 * accessors ({@code mergeFrom}, {@code clearField}, {@code *Bytes}, ...) that would each
 * need an explicit {@code @Mapping(target = "...", ignore = true)}.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcDeviceBuilder {

    public GrpcPageDeviceQuery toGrpcPageQuery(FacadeDeviceQuery query) {
        GrpcPageDeviceQuery.Builder builder = GrpcPageDeviceQuery.newBuilder();

        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        GrpcPage.Builder page = GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize());
        builder.setPage(page);

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getDeviceName()).ifPresent(builder::setDeviceName);
        StringOptional.ofNullable(query.getDeviceCode()).ifPresent(builder::setDeviceCode);
        LongOptional.ofNullable(query.getDriverId()).ifPresent(builder::setDriverId);
        LongOptional.ofNullable(query.getProfileId()).ifPresent(builder::setProfileId);
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));

        return builder.build();
    }

    public FacadeDeviceBO toFacadeBO(GrpcDeviceDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeDeviceBO bo = new FacadeDeviceBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getDeviceName()).ifPresent(bo::setDeviceName);
        StringOptional.ofNullable(dto.getDeviceCode()).ifPresent(bo::setDeviceCode);
        LongOptional.ofNullable(dto.getDriverId()).ifPresent(bo::setDriverId);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);

        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }

        byte enableIndex = (byte) dto.getEnableFlag();
        Optional.ofNullable(EnableFlagEnum.ofIndex(enableIndex)).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getDeviceExt())
                .ifPresent(value -> bo.setDeviceExt(JsonUtil.parseObject(value, DeviceExt.class)));

        if (dto.getProfileIdsCount() > 0) {
            bo.setProfileId(dto.getProfileIdsList().stream().findFirst().orElse(null));
        }

        return bo;
    }

}
