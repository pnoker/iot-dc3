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

import io.github.pnoker.api.center.manager.GrpcOffsetCommandQuery;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.enums.CallTypeEnum;
import io.github.pnoker.common.enums.CommandTypeEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandOffsetQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Hand-rolled conversion between facade shapes and protobuf command types.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcCommandBuilder {

    public GrpcOffsetCommandQuery toGrpcOffsetQuery(FacadeCommandOffsetQuery query) {
        PageRequest.Builder page =
                PageRequest.newBuilder().setOffset(query.offset()).setLimit(query.limit());
        if (query.sort() != null) {
            query.sort()
                    .forEach(spec -> page.addSort(io.github.pnoker.api.common.SortSpec.newBuilder()
                            .setField(spec.field())
                            .setDirection(
                                    spec.direction() == io.github.pnoker.db.r2dbc.core.page.SortSpec.Direction.DESC
                                            ? SortDirection.SORT_DIRECTION_DESC
                                            : SortDirection.SORT_DIRECTION_ASC)
                            .build()));
        }
        GrpcOffsetCommandQuery.Builder builder =
                GrpcOffsetCommandQuery.newBuilder().setPage(page).setTenantId(query.tenantId());
        Optional.ofNullable(query.commandName()).ifPresent(builder::setCommandName);
        Optional.ofNullable(query.commandCode()).ifPresent(builder::setCommandCode);
        Optional.ofNullable(query.commandTypeFlag()).ifPresent(value -> builder.setCommandTypeFlag(value.getIndex()));
        Optional.ofNullable(query.callTypeFlag()).ifPresent(value -> builder.setCallTypeFlag(value.getIndex()));
        Optional.ofNullable(query.profileId()).ifPresent(builder::setProfileId);
        Optional.ofNullable(query.enableFlag()).ifPresent(value -> builder.setEnableFlag(value.getIndex()));
        Optional.ofNullable(query.version()).ifPresent(builder::setVersion);
        Optional.ofNullable(query.deviceId()).ifPresent(builder::setDeviceId);
        return builder.build();
    }

    /**
     * To facade business object.
     *
     * @param dto dto
     * @return to facade business object result
     */
    public FacadeCommandBO toFacadeBO(GrpcCommandDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeCommandBO bo = new FacadeCommandBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getCommandName()).ifPresent(bo::setCommandName);
        StringOptional.ofNullable(dto.getCommandCode()).ifPresent(bo::setCommandCode);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);
        LongOptional.ofNullable(dto.getProfileId()).ifPresent(bo::setProfileId);

        bo.setTimeout(dto.getTimeout());

        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }

        int commandType = dto.getCommandTypeFlag();
        if (commandType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(CommandTypeEnum.ofIndex((byte) commandType)).ifPresent(bo::setCommandTypeFlag);
        }

        int callType = dto.getCallTypeFlag();
        if (callType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(CallTypeEnum.ofIndex((byte) callType)).ifPresent(bo::setCallTypeFlag);
        }

        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getCommandExt())
                .ifPresent(value -> bo.setCommandExt(JsonUtil.parseObject(value, CommandExt.class)));

        return bo;
    }
}
