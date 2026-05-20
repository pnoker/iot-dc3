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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.vo.NotifyChannelBindVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.NotifyChannelBindExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * MapStruct builder converting between notification channel binding BO, VO, and DO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface NotifyChannelBindBuilder {

    @Mapping(target = "tenantId", ignore = true)
    NotifyChannelBindBO buildBOByVO(NotifyChannelBindVO entityVO);

    List<NotifyChannelBindBO> buildBOListByVOList(List<NotifyChannelBindVO> entityVOList);

    @Mapping(target = "bindExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    NotifyChannelBindDO buildDOByBO(NotifyChannelBindBO entityBO);

    @AfterMapping
    default void afterProcess(NotifyChannelBindBO entityBO, @MappingTarget NotifyChannelBindDO entityDO) {
        NotifyChannelBindExt entityExt = entityBO.getBindExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setBindExt(ext);

        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    List<NotifyChannelBindDO> buildDOListByBOList(List<NotifyChannelBindBO> entityBOList);

    @Mapping(target = "bindExt", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    NotifyChannelBindBO buildBOByDO(NotifyChannelBindDO entityDO);

    @AfterMapping
    default void afterProcess(NotifyChannelBindDO entityDO, @MappingTarget NotifyChannelBindBO entityBO) {
        JsonExt entityExt = entityDO.getBindExt();
        if (Objects.nonNull(entityExt)) {
            NotifyChannelBindExt ext = new NotifyChannelBindExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), NotifyChannelBindExt.Content.class));
            entityBO.setBindExt(ext);
        }

        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    List<NotifyChannelBindBO> buildBOListByDOList(List<NotifyChannelBindDO> entityDOList);

    NotifyChannelBindVO buildVOByBO(NotifyChannelBindBO entityBO);

    List<NotifyChannelBindVO> buildVOListByBOList(List<NotifyChannelBindBO> entityBOList);

    default Page<NotifyChannelBindBO> buildBOPageByDOPage(Page<NotifyChannelBindDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    default Page<NotifyChannelBindVO> buildVOPageByBOPage(Page<NotifyChannelBindBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
