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

import io.github.pnoker.api.center.manager.GrpcPageEventQuery;
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.ext.EventExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

/**
 * Hand-rolled conversion between facade shapes and protobuf event types.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcEventBuilder {

    public GrpcPageEventQuery toGrpcPageQuery(FacadeEventQuery query) {
        GrpcPageEventQuery.Builder builder = GrpcPageEventQuery.newBuilder();

        Pages pages = Objects.isNull(query.getPage()) ? new Pages() : query.getPage();
        GrpcPage.Builder page = GrpcPage.newBuilder().setCurrent(pages.getCurrent()).setSize(pages.getSize());
        builder.setPage(page);

        LongOptional.ofNullable(query.getTenantId()).ifPresent(builder::setTenantId);
        StringOptional.ofNullable(query.getEventName()).ifPresent(builder::setEventName);
        StringOptional.ofNullable(query.getEventCode()).ifPresent(builder::setEventCode);
        LongOptional.ofNullable(query.getDeviceId()).ifPresent(builder::setDeviceId);

        Optional.ofNullable(query.getEventType())
                .ifPresentOrElse(value -> builder.setEventTypeFlag(value.getIndex()),
                        () -> builder.setEventTypeFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEventLevel())
                .ifPresentOrElse(value -> builder.setEventLevelFlag(value.getIndex()),
                        () -> builder.setEventLevelFlag(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getProfileId())
                .ifPresentOrElse(builder::setProfileId, () -> builder.setProfileId(DefaultConstant.NULL_INT));
        Optional.ofNullable(query.getEnableFlag())
                .ifPresentOrElse(value -> builder.setEnableFlag(value.getIndex()),
                        () -> builder.setEnableFlag(DefaultConstant.NULL_INT));

        return builder.build();
    }

    public FacadeEventBO toFacadeBO(GrpcEventDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadeEventBO bo = new FacadeEventBO();
        GrpcBuilderUtil.buildBaseBOByGrpcBase(dto.getBase(), bo);

        StringOptional.ofNullable(dto.getEventName()).ifPresent(bo::setEventName);
        StringOptional.ofNullable(dto.getEventCode()).ifPresent(bo::setEventCode);
        StringOptional.ofNullable(dto.getSignature()).ifPresent(bo::setSignature);
        LongOptional.ofNullable(dto.getTenantId()).ifPresent(bo::setTenantId);
        LongOptional.ofNullable(dto.getProfileId()).ifPresent(bo::setProfileId);

        if (dto.getVersion() != DefaultConstant.DEFAULT_INT) {
            bo.setVersion(dto.getVersion());
        }

        int eventType = dto.getEventTypeFlag();
        if (eventType != DefaultConstant.NULL_INT) {
            Optional.ofNullable(EventTypeFlagEnum.ofIndex((byte) eventType)).ifPresent(bo::setEventTypeFlag);
        }

        int eventLevel = dto.getEventLevelFlag();
        if (eventLevel != DefaultConstant.NULL_INT) {
            Optional.ofNullable(EventLevelEnum.ofIndex((byte) eventLevel)).ifPresent(bo::setEventLevelFlag);
        }

        Optional.ofNullable(EnableFlagEnum.ofIndex((byte) dto.getEnableFlag())).ifPresent(bo::setEnableFlag);

        StringOptional.ofNullable(dto.getEventExt())
                .ifPresent(value -> bo.setEventExt(JsonUtil.parseObject(value, EventExt.class)));

        return bo;
    }

}
