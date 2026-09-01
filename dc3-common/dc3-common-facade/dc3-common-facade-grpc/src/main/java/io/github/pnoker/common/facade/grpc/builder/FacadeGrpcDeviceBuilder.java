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

import io.github.pnoker.api.center.manager.GrpcOffsetDeviceQuery;
import io.github.pnoker.api.common.GrpcDeviceDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.DeviceExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceOffsetQuery;
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
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcDeviceBuilder {

    /** Convert the canonical facade query without dropping sort information. */
    public GrpcOffsetDeviceQuery toGrpcOffsetQuery(FacadeDeviceOffsetQuery query) {
        PageRequest.Builder page = PageRequest.newBuilder()
                .setOffset(query.offset()).setLimit(query.limit());
        query.sort().forEach(spec -> page.addSort(io.github.pnoker.api.common.SortSpec.newBuilder()
                .setField(spec.field())
                .setDirection(spec.direction() == io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.DESC
                        ? io.github.pnoker.api.common.SortDirection.SORT_DIRECTION_DESC
                        : io.github.pnoker.api.common.SortDirection.SORT_DIRECTION_ASC)
                .build()));
        GrpcOffsetDeviceQuery.Builder builder = GrpcOffsetDeviceQuery.newBuilder()
                .setTenantId(query.tenantId()).setPage(page.build());
        StringOptional.ofNullable(query.deviceName()).ifPresent(builder::setDeviceName);
        StringOptional.ofNullable(query.deviceCode()).ifPresent(builder::setDeviceCode);
        LongOptional.ofNullable(query.driverId()).ifPresent(builder::setDriverId);
        LongOptional.ofNullable(query.profileId()).ifPresent(builder::setProfileId);
        Optional.ofNullable(query.enableFlag()).ifPresent(value -> builder.setEnableFlag(value.getIndex()));
        Optional.ofNullable(query.version()).ifPresent(builder::setVersion);
        LongOptional.ofNullable(query.groupId()).ifPresent(builder::setGroupId);
        LongOptional.ofNullable(query.labelId()).ifPresent(builder::setLabelId);
        return builder.build();
    }

    /**
     * To facade business object.
     *
     * @param dto dto
     * @return to facade business object result
     */
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
