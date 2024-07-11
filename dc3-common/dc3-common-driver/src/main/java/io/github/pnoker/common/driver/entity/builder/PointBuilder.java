/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.driver.entity.builder;

import io.github.pnoker.api.common.GrpcPointDTO;
import io.github.pnoker.common.driver.entity.bo.PointBO;
import io.github.pnoker.common.entity.ext.PointExt;
import io.github.pnoker.common.enums.PointTypeFlagEnum;
import io.github.pnoker.common.enums.RwFlagEnum;
import io.github.pnoker.common.optional.EnableOptional;
import io.github.pnoker.common.optional.JsonOptional;
import io.github.pnoker.common.utils.GrpcBuilderUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

/**
 * Point Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PointBuilder {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    @Mapping(target = "pointExt", ignore = true)
    @Mapping(target = "rwFlag", ignore = true)
    @Mapping(target = "pointTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    PointBO buildDTOByGrpcDTO(GrpcPointDTO entityGrpc);

    @AfterMapping
    default void afterProcess(GrpcPointDTO entityGrpc, @MappingTarget PointBO entityBO) {
        GrpcBuilderUtil.buildBaseBOByGrpcBase(entityGrpc.getBase(), entityBO);

        JsonOptional.ofNullable(entityGrpc.getPointExt()).ifPresent(value -> entityBO.setPointExt(JsonUtil.parseObject(value, PointExt.class)));
        Optional.ofNullable(RwFlagEnum.ofIndex((byte) entityGrpc.getRwFlag())).ifPresent(entityBO::setRwFlag);
        Optional.ofNullable(PointTypeFlagEnum.ofIndex((byte) entityGrpc.getPointTypeFlag())).ifPresent(entityBO::setPointTypeFlag);
        EnableOptional.ofNullable(entityGrpc.getEnableFlag()).ifPresent(entityBO::setEnableFlag);
    }
}