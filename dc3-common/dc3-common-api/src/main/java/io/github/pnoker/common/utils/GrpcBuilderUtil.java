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

package io.github.pnoker.common.utils;

import io.github.pnoker.api.common.GrpcBase;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.entity.base.BaseBO;
import io.github.pnoker.common.entity.base.BaseDTO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.optional.LongOptional;
import io.github.pnoker.common.optional.StringOptional;

import java.util.Objects;
import java.util.Optional;

/**
 * Grpc Builder 工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
public class GrpcBuilderUtil {

    private GrpcBuilderUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Grpc Page to Pages
     *
     * @param page GrpcPage
     * @return Pages
     */
    public static Pages buildPagesByGrpcPage(GrpcPage page) {
        if (Objects.isNull(page)) {
            GrpcPage.Builder builder = GrpcPage.newBuilder();
            builder.setCurrent(1);
            builder.setPages(DefaultConstant.PAGE_SIZE);
            page = builder.build();
        }

        Pages pages = new Pages();
        long current = page.getCurrent() < 1 ? 1 : page.getCurrent();
        long pageSize = page.getSize() < 1 ? DefaultConstant.PAGE_SIZE : page.getSize();
        pageSize = pageSize > DefaultConstant.MAX_PAGE_SIZE ? DefaultConstant.MAX_PAGE_SIZE : pageSize;
        pages.setCurrent(current);
        pages.setSize(pageSize);
        return pages;
    }

    /**
     * Entity Base BO to Grpc Base BO
     *
     * @param entityBO EntityBO
     * @param <T>      EntityBO extends BaseBO
     * @return GrpcBase
     */
    public static <T extends BaseBO> GrpcBase buildGrpcBaseByBO(T entityBO) {
        if (Objects.isNull(entityBO)) {
            return null;
        }

        GrpcBase.Builder builder = GrpcBase.newBuilder();
        Optional.ofNullable(entityBO.getId()).ifPresentOrElse(builder::setId, () -> builder.setId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getRemark()).ifPresent(builder::setRemark);
        Optional.ofNullable(entityBO.getCreatorId()).ifPresentOrElse(builder::setCreatorId, () -> builder.setCreatorId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getCreatorName()).ifPresent(builder::setCreatorName);
        Optional.ofNullable(entityBO.getCreateTime()).ifPresent(value -> builder.setCreateTime(LocalDateTimeUtil.milliSeconds(value)));
        Optional.ofNullable(entityBO.getOperatorId()).ifPresentOrElse(builder::setOperatorId, () -> builder.setOperatorId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityBO.getOperatorName()).ifPresent(builder::setOperatorName);
        Optional.ofNullable(entityBO.getOperateTime()).ifPresent(value -> builder.setOperateTime(LocalDateTimeUtil.milliSeconds(value)));
        return builder.build();
    }

    /**
     * Grpc Base to Base BO
     *
     * @param entityGrpc GrpcBase
     * @param entityBO   EntityBO
     * @param <T>        EntityBO extends BaseBO
     */
    public static <T extends BaseBO> void buildBaseBOByGrpcBase(GrpcBase entityGrpc, T entityBO) {
        if (Objects.isNull(entityGrpc)) {
            return;
        }

        LongOptional.ofNullable(entityGrpc.getId()).ifPresent(entityBO::setId);
        StringOptional.ofNullable(entityGrpc.getRemark()).ifPresent(entityBO::setRemark);
        LongOptional.ofNullable(entityGrpc.getCreatorId()).ifPresent(entityBO::setCreatorId);
        StringOptional.ofNullable(entityGrpc.getCreatorName()).ifPresent(entityBO::setCreatorName);
        LongOptional.ofNullable(entityGrpc.getCreateTime()).ifPresent(value -> entityBO.setCreateTime(LocalDateTimeUtil.localDateTime(value)));
        LongOptional.ofNullable(entityGrpc.getOperatorId()).ifPresent(entityBO::setOperatorId);
        StringOptional.ofNullable(entityGrpc.getOperatorName()).ifPresent(entityBO::setOperatorName);
        LongOptional.ofNullable(entityGrpc.getOperateTime()).ifPresent(value -> entityBO.setOperateTime(LocalDateTimeUtil.localDateTime(value)));
    }

    /**
     * Entity Base DTO to Grpc Base DTO
     *
     * @param entityDTO EntityDTO
     * @param <T>       EntityDTO extends BaseDTO
     * @return GrpcBase
     */
    public static <T extends BaseDTO> GrpcBase buildGrpcBaseByDTO(T entityDTO) {
        if (Objects.isNull(entityDTO)) {
            return null;
        }

        GrpcBase.Builder builder = GrpcBase.newBuilder();
        Optional.ofNullable(entityDTO.getId()).ifPresentOrElse(builder::setId, () -> builder.setId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityDTO.getRemark()).ifPresent(builder::setRemark);
        Optional.ofNullable(entityDTO.getCreatorId()).ifPresentOrElse(builder::setCreatorId, () -> builder.setCreatorId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityDTO.getCreatorName()).ifPresent(builder::setCreatorName);
        Optional.ofNullable(entityDTO.getCreateTime()).ifPresent(value -> builder.setCreateTime(LocalDateTimeUtil.milliSeconds(value)));
        Optional.ofNullable(entityDTO.getOperatorId()).ifPresentOrElse(builder::setOperatorId, () -> builder.setOperatorId(DefaultConstant.NULL_INT));
        Optional.ofNullable(entityDTO.getOperatorName()).ifPresent(builder::setOperatorName);
        Optional.ofNullable(entityDTO.getOperateTime()).ifPresent(time -> builder.setOperateTime(LocalDateTimeUtil.milliSeconds(time)));
        return builder.build();
    }

    /**
     * Grpc Base to Base DTO
     *
     * @param entityGrpc GrpcBase
     * @param entityDTO  EntityDTO
     * @param <T>        EntityDTO extends GrpcBase
     */
    public static <T extends BaseDTO> void buildBaseDTOByGrpcBase(GrpcBase entityGrpc, T entityDTO) {
        if (Objects.isNull(entityGrpc)) {
            return;
        }

        LongOptional.ofNullable(entityGrpc.getId()).ifPresent(entityDTO::setId);
        StringOptional.ofNullable(entityGrpc.getRemark()).ifPresent(entityDTO::setRemark);
        LongOptional.ofNullable(entityGrpc.getCreatorId()).ifPresent(entityDTO::setCreatorId);
        StringOptional.ofNullable(entityGrpc.getCreatorName()).ifPresent(entityDTO::setCreatorName);
        LongOptional.ofNullable(entityGrpc.getCreateTime()).ifPresent(value -> entityDTO.setCreateTime(LocalDateTimeUtil.localDateTime(value)));
        LongOptional.ofNullable(entityGrpc.getOperatorId()).ifPresent(entityDTO::setOperatorId);
        StringOptional.ofNullable(entityGrpc.getOperatorName()).ifPresent(entityDTO::setOperatorName);
        LongOptional.ofNullable(entityGrpc.getOperateTime()).ifPresent(value -> entityDTO.setOperateTime(LocalDateTimeUtil.localDateTime(value)));
    }

}
