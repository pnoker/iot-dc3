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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.vo.NotifyChannelVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyChannelExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.NotifyChannelTypeEnum;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * MapStruct builder converting between notification channel BO, VO, and DO.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface NotifyChannelBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "tenantId", ignore = true)
    NotifyChannelBO buildBOByVO(NotifyChannelVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<NotifyChannelBO> buildBOListByVOList(List<NotifyChannelVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "channelExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    NotifyChannelDO buildDOByBO(NotifyChannelBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(NotifyChannelBO entityBO, @MappingTarget NotifyChannelDO entityDO) {
        if (StringUtils.isEmpty(entityBO.getChannelCode())) {
            entityDO.setChannelCode(CodeUtil.getCode());
        }

        NotifyChannelExt entityExt = entityBO.getChannelExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setChannelExt(ext);

        NotifyChannelTypeEnum channelTypeFlag = entityBO.getChannelTypeFlag();
        Optional.ofNullable(channelTypeFlag).ifPresent(value -> entityDO.setChannelTypeFlag(value.getIndex()));

        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<NotifyChannelDO> buildDOListByBOList(List<NotifyChannelBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "channelExt", ignore = true)
    @Mapping(target = "channelTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    NotifyChannelBO buildBOByDO(NotifyChannelDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(NotifyChannelDO entityDO, @MappingTarget NotifyChannelBO entityBO) {
        JsonExt entityExt = entityDO.getChannelExt();
        if (Objects.nonNull(entityExt)) {
            NotifyChannelExt ext = new NotifyChannelExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), NotifyChannelExt.Content.class));
            entityBO.setChannelExt(ext);
        }

        Byte channelTypeFlag = entityDO.getChannelTypeFlag();
        entityBO.setChannelTypeFlag(NotifyChannelTypeEnum.ofIndex(channelTypeFlag));

        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<NotifyChannelBO> buildBOListByDOList(List<NotifyChannelDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    NotifyChannelVO buildVOByBO(NotifyChannelBO entityBO);

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<NotifyChannelVO> buildVOListByBOList(List<NotifyChannelBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<NotifyChannelBO> buildBOPageByDOPage(Page<NotifyChannelDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<NotifyChannelVO> buildVOPageByBOPage(Page<NotifyChannelBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
