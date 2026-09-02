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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct builder converting between notification delivery history BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface NotifyHistoryBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "dedupeKey", ignore = true)
    NotifyHistoryBO buildBOByVO(NotifyHistoryVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<NotifyHistoryBO> buildBOListByVOList(List<NotifyHistoryVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    NotifyHistoryDO buildDOByBO(NotifyHistoryBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(NotifyHistoryBO entityBO, @MappingTarget NotifyHistoryDO entityDO) {
        entityDO.setRequestExt(buildRequestExt(entityBO.getRequestExt()));
        entityDO.setResponseExt(buildResponseExt(entityBO.getResponseExt()));

        NotifyChannelTypeEnum channelTypeFlag = entityBO.getChannelTypeFlag();
        Optional.ofNullable(channelTypeFlag).ifPresent(value -> entityDO.setChannelTypeFlag(value.getIndex()));

        NotifyHistoryStatusEnum statusFlag = entityBO.getStatusFlag();
        Optional.ofNullable(statusFlag).ifPresent(value -> entityDO.setStatusFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<NotifyHistoryDO> buildDOListByBOList(List<NotifyHistoryBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "requestExt", ignore = true)
    @Mapping(target = "responseExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "statusFlag", ignore = true)
    NotifyHistoryBO buildBOByDO(NotifyHistoryDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
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

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<NotifyHistoryBO> buildBOListByDOList(List<NotifyHistoryDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    NotifyHistoryVO buildVOByBO(NotifyHistoryBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<NotifyHistoryVO> buildVOListByBOList(List<NotifyHistoryBO> entityBOList);

    /**
     * Build request ext.
     *
     * @param entityExt entity ext
     * @return converted value
     */
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

    /**
     * Build response ext.
     *
     * @param entityExt entity ext
     * @return converted value
     */
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
