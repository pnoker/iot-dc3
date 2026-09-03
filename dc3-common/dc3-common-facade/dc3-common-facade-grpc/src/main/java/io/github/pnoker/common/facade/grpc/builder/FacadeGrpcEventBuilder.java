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

import io.github.pnoker.api.center.manager.GrpcOffsetEventQuery;
import io.github.pnoker.api.common.GrpcEventDTO;
import io.github.pnoker.api.common.PageRequest;
import io.github.pnoker.api.common.SortDirection;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.entity.ext.EventExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.facade.entity.bo.FacadeEventBO;
import io.github.pnoker.common.facade.entity.query.FacadeEventOffsetQuery;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Hand-rolled conversion between facade shapes and protobuf event types.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Component
public class FacadeGrpcEventBuilder {

    /** Convert the offset query to its gRPC form. */
    public GrpcOffsetEventQuery toGrpcOffsetQuery(FacadeEventOffsetQuery query) {
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
        GrpcOffsetEventQuery.Builder builder =
                GrpcOffsetEventQuery.newBuilder().setPage(page).setTenantId(query.tenantId());
        Optional.ofNullable(query.eventName()).ifPresent(builder::setEventName);
        Optional.ofNullable(query.eventCode()).ifPresent(builder::setEventCode);
        Optional.ofNullable(query.eventTypeFlag()).ifPresent(value -> builder.setEventTypeFlag(value.getIndex()));
        Optional.ofNullable(query.eventLevelFlag()).ifPresent(value -> builder.setEventLevelFlag(value.getIndex()));
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
