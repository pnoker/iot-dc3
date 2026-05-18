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
import io.github.pnoker.common.data.entity.bo.NotifyRecordBO;
import io.github.pnoker.common.data.entity.model.NotifyRecordDO;
import io.github.pnoker.common.data.entity.vo.NotifyRecordVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyRecordRequestExt;
import io.github.pnoker.common.entity.ext.NotifyRecordResponseExt;
import io.github.pnoker.common.enums.NotifyChannelTypeFlagEnum;
import io.github.pnoker.common.enums.NotifyRecordStatusEnum;
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
 * Notification delivery record builder.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface NotifyRecordBuilder {

    @Mapping(target = "tenantId", ignore = true)
    NotifyRecordBO buildBOByVO(NotifyRecordVO entityVO);

    List<NotifyRecordBO> buildBOListByVOList(List<NotifyRecordVO> entityVOList);

    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    NotifyRecordDO buildDOByBO(NotifyRecordBO entityBO);

    @AfterMapping
    default void afterProcess(NotifyRecordBO entityBO, @MappingTarget NotifyRecordDO entityDO) {
        entityDO.setRequestExt(buildRequestExt(entityBO.getRequestExt()));
        entityDO.setResponseExt(buildResponseExt(entityBO.getResponseExt()));

        NotifyChannelTypeFlagEnum channelTypeFlag = entityBO.getChannelTypeFlag();
        Optional.ofNullable(channelTypeFlag).ifPresent(value -> entityDO.setChannelTypeFlag(value.getIndex()));

        NotifyRecordStatusEnum statusFlag = entityBO.getStatusFlag();
        Optional.ofNullable(statusFlag).ifPresent(value -> entityDO.setStatusFlag(value.getIndex()));
    }

    List<NotifyRecordDO> buildDOListByBOList(List<NotifyRecordBO> entityBOList);

    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    NotifyRecordBO buildBOByDO(NotifyRecordDO entityDO);

    @AfterMapping
    default void afterProcess(NotifyRecordDO entityDO, @MappingTarget NotifyRecordBO entityBO) {
        JsonExt requestExt = entityDO.getRequestExt();
        if (Objects.nonNull(requestExt)) {
            NotifyRecordRequestExt ext = new NotifyRecordRequestExt();
            ext.setType(requestExt.getType());
            ext.setVersion(requestExt.getVersion());
            ext.setRemark(requestExt.getRemark());
            ext.setContent(JsonUtil.parseObject(requestExt.getContent(), NotifyRecordRequestExt.Content.class));
            entityBO.setRequestExt(ext);
        }

        JsonExt responseExt = entityDO.getResponseExt();
        if (Objects.nonNull(responseExt)) {
            NotifyRecordResponseExt ext = new NotifyRecordResponseExt();
            ext.setType(responseExt.getType());
            ext.setVersion(responseExt.getVersion());
            ext.setRemark(responseExt.getRemark());
            ext.setContent(JsonUtil.parseObject(responseExt.getContent(), NotifyRecordResponseExt.Content.class));
            entityBO.setResponseExt(ext);
        }

        Byte channelTypeFlag = entityDO.getChannelTypeFlag();
        entityBO.setChannelTypeFlag(NotifyChannelTypeFlagEnum.ofIndex(channelTypeFlag));

        Byte statusFlag = entityDO.getStatusFlag();
        entityBO.setStatusFlag(NotifyRecordStatusEnum.ofIndex(statusFlag));
    }

    List<NotifyRecordBO> buildBOListByDOList(List<NotifyRecordDO> entityDOList);

    NotifyRecordVO buildVOByBO(NotifyRecordBO entityBO);

    List<NotifyRecordVO> buildVOListByBOList(List<NotifyRecordBO> entityBOList);

    default Page<NotifyRecordBO> buildBOPageByDOPage(Page<NotifyRecordDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<NotifyRecordVO> buildVOPageByBOPage(Page<NotifyRecordBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

    default JsonExt buildRequestExt(NotifyRecordRequestExt entityExt) {
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        return ext;
    }

    default JsonExt buildResponseExt(NotifyRecordResponseExt entityExt) {
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
