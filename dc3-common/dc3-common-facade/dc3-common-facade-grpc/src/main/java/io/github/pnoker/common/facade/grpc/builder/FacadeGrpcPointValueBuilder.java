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

import io.github.pnoker.api.center.data.GrpcPointValueDTO;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.optional.StringOptional;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Converts between {@code dc3-common-facade-api} shapes and the protobuf types
 * generated from {@code api/center/data/point_value.proto}.
 * <p>
 * Hand-rolled rather than MapStruct because protobuf builders expose dozens of
 * generated accessors that would each need an explicit ignore mapping.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Component
public class FacadeGrpcPointValueBuilder {

    /**
     * Convert a gRPC {@link GrpcPointValueDTO} to a facade-level BO.
     *
     * @param dto the gRPC DTO (may be {@code null})
     * @return the facade BO, or {@code null} when the input is {@code null}
     */
    public FacadePointValueBO toFacadeBO(GrpcPointValueDTO dto) {
        if (Objects.isNull(dto)) {
            return null;
        }

        FacadePointValueBO bo = FacadePointValueBO.builder()
                .createTime(dto.getCreateTime())
                .build();

        if (dto.getDeviceId() > 0) {
            bo.setDeviceId(dto.getDeviceId());
        }
        if (dto.getPointId() > 0) {
            bo.setPointId(dto.getPointId());
        }
        StringOptional.ofNullable(dto.getValue()).ifPresent(bo::setValue);
        StringOptional.ofNullable(dto.getRawValue()).ifPresent(bo::setRawValue);

        return bo;
    }
}
