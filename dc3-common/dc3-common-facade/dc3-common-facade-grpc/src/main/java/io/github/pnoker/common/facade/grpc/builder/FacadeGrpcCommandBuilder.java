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

import io.github.pnoker.api.center.manager.GrpcPageCommandQuery;
import io.github.pnoker.api.common.GrpcCommandDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.CommandExt;
import io.github.pnoker.common.enums.CallTypeFlagEnum;
import io.github.pnoker.common.enums.CommandTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeCommandBO;
import io.github.pnoker.common.facade.entity.query.FacadeCommandQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Hand-rolled conversion between facade shapes and protobuf command types.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcCommandBuilder {

    public GrpcPageCommandQuery toGrpcPageQuery(FacadeCommandQuery query) {
        GrpcPageCommandQuery.Builder builder = GrpcPageCommandQuery.newBuilder();

        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        GrpcPage.Builder page = GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize());
        builder.setPage(page);

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getCommandName()).ifPresent(builder::setCommandName);
        StringOptional.ofNullable(query.getCommandCode()).ifPresent(builder::setCommandCode);
        LongOptional.ofNullable(query.getDeviceId()).ifPresent(builder::setDeviceId);

        Optional.ofNullable(query.getCommandType())
                .ifPresentOrElse(value -> builder.setCommandTypeFlag(value.getIndex()),
                        () -> builder.setCommandTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getCallType())
                .ifPresentOrElse(value -> builder.setCallTypeFlag(value.getIndex()),
                        () -> builder.setCallTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getProfileId())
                .ifPresentOrElse(builder::setProfileId, () -> builder.setProfileId(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));

        return builder.build();
    }

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
            Optional.ofNullable(CommandTypeFlagEnum.ofIndex((byte) commandType)).ifPresent(bo::setCommandTypeFlag);
        }

        int callType = dto.getCallTypeFlag();
        if (callType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(CallTypeFlagEnum.ofIndex((byte) callType)).ifPresent(bo::setCallTypeFlag);
        }

        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getCommandExt())
                .ifPresent(value -> bo.setCommandExt(JsonUtil.parseObject(value, CommandExt.class)));

        return bo;
    }

}
