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

package io.github.pnoker.common.data.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.bo.NotifyHistoryBO;
import io.github.pnoker.common.data.entity.model.NotifyHistoryDO;
import io.github.pnoker.common.data.entity.vo.NotifyHistoryVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryRequestExt;
import io.github.pnoker.common.entity.ext.NotifyHistoryResponseExt;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between notification delivery history BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface NotifyHistoryBuilder {

    @Mapping(target = "tenantId", ignore = true)
    NotifyHistoryBO buildBOByVO(NotifyHistoryVO entityVO);

    List<NotifyHistoryBO> buildBOListByVOList(List<NotifyHistoryVO> entityVOList);

    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    NotifyHistoryDO buildDOByBO(NotifyHistoryBO entityBO);

    @AfterMapping
    default void afterProcess(NotifyHistoryBO entityBO, @MappingTarget NotifyHistoryDO entityDO) {
        entityDO.setRequestExt(buildRequestExt(entityBO.getRequestExt()));
        entityDO.setResponseExt(buildResponseExt(entityBO.getResponseExt()));

        NotifyChannelTypeEnum channelTypeFlag = entityBO.getChannelTypeFlag();
        Optional.ofNullable(channelTypeFlag).ifPresent(value -> entityDO.setChannelTypeFlag(value.getIndex()));

        NotifyHistoryStatusEnum statusFlag = entityBO.getStatusFlag();
        Optional.ofNullable(statusFlag).ifPresent(value -> entityDO.setStatusFlag(value.getIndex()));
    }

    List<NotifyHistoryDO> buildDOListByBOList(List<NotifyHistoryBO> entityBOList);

    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    NotifyHistoryBO buildBOByDO(NotifyHistoryDO entityDO);

    @AfterMapping
    default void afterProcess(NotifyHistoryDO entityDO, @MappingTarget NotifyHistoryBO entityBO) {
        JsonExt requestExt = entityDO.getRequestExt();
        if (Objects.nonNull(requestExt)) {
            NotifyHistoryRequestExt ext = new NotifyHistoryRequestExt();
            ext.setType(requestExt.getType());
            ext.setVersion(requestExt.getVersion());
            ext.setRemark(requestExt.getRemark());
            ext.setContent(JsonUtil.parseObject(requestExt.getContent(), NotifyHistoryRequestExt.Content.class));
            entityBO.setRequestExt(ext);
        }

        JsonExt responseExt = entityDO.getResponseExt();
        if (Objects.nonNull(responseExt)) {
            NotifyHistoryResponseExt ext = new NotifyHistoryResponseExt();
            ext.setType(responseExt.getType());
            ext.setVersion(responseExt.getVersion());
            ext.setRemark(responseExt.getRemark());
            ext.setContent(JsonUtil.parseObject(responseExt.getContent(), NotifyHistoryResponseExt.Content.class));
            entityBO.setResponseExt(ext);
        }

        Byte channelTypeFlag = entityDO.getChannelTypeFlag();
        entityBO.setChannelTypeFlag(NotifyChannelTypeEnum.ofIndex(channelTypeFlag));

        Byte statusFlag = entityDO.getStatusFlag();
        entityBO.setStatusFlag(NotifyHistoryStatusEnum.ofIndex(statusFlag));
    }

    List<NotifyHistoryBO> buildBOListByDOList(List<NotifyHistoryDO> entityDOList);

    NotifyHistoryVO buildVOByBO(NotifyHistoryBO entityBO);

    List<NotifyHistoryVO> buildVOListByBOList(List<NotifyHistoryBO> entityBOList);

    default Page<NotifyHistoryBO> buildBOPageByDOPage(Page<NotifyHistoryDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<NotifyHistoryVO> buildVOPageByBOPage(Page<NotifyHistoryBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    default JsonExt buildRequestExt(NotifyHistoryRequestExt entityExt) {
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        return ext;
    }

    default JsonExt buildResponseExt(NotifyHistoryResponseExt entityExt) {
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        return ext;
    }

}
