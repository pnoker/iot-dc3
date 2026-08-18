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
package io.github.pnoker.common.agentic.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.MessageBO;
import io.github.pnoker.common.agentic.entity.model.AgenticMessageContent;
import io.github.pnoker.common.agentic.entity.model.MessageDO;
import io.github.pnoker.common.agentic.entity.vo.MessageVO;
import io.github.pnoker.common.enums.AgenticMessageStatusEnum;
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
 * MapStruct builder converting between message BO, VO, and DO.
 *
 * @author pnoker
 * @since 2026.5.11
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface MessageBuilder {

    /**
     * Convert vo to bo.
     *
     * @param entityVO view object
     * @return converted value
     */
    @Mapping(target = "content", source = "contentExt")
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    MessageBO buildBOByVO(MessageVO entityVO);

    /**
     * Convert vo list to bo list.
     *
     * @param entityVOList entity view object list
     * @return converted value
     */
    List<MessageBO> buildBOListByVOList(List<MessageVO> entityVOList);

    /**
     * Convert bo to do.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "status", ignore = true)
    MessageDO buildDOByBO(MessageBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityDO persistence object
     */
    @AfterMapping
    default void afterProcess(MessageBO entityBO, @MappingTarget MessageDO entityDO) {
        AgenticMessageStatusEnum status = entityBO.getStatus();
        Optional.ofNullable(status).ifPresent(value -> entityDO.setStatus(value.getIndex()));
    }

    /**
     * Convert bo list to do list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<MessageDO> buildDOListByBOList(List<MessageBO> entityBOList);

    /**
     * Convert do to bo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    @Mapping(target = "status", ignore = true)
    MessageBO buildBOByDO(MessageDO entityDO);

    /**
     * After process.
     *
     * @param entityDO persistence object
     * @param entityBO business object
     */
    @AfterMapping
    default void afterProcess(MessageDO entityDO, @MappingTarget MessageBO entityBO) {
        Byte status = entityDO.getStatus();
        entityBO.setStatus(AgenticMessageStatusEnum.ofIndex(status));
    }

    /**
     * Convert do list to bo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<MessageBO> buildBOListByDOList(List<MessageDO> entityDOList);

    /**
     * Convert bo to vo.
     *
     * @param entityBO business object
     * @return converted value
     */
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "contentExt", source = "content")
    MessageVO buildVOByBO(MessageBO entityBO);

    /**
     * After process.
     *
     * @param entityBO business object
     * @param entityVO view object
     */
    @AfterMapping
    default void afterProcess(MessageBO entityBO, @MappingTarget MessageVO entityVO) {
        AgenticMessageContent content = Objects.nonNull(entityBO.getContent()) ? entityBO.getContent()
                : AgenticMessageContent.ofText("");
        entityVO.setContent(StringUtils.defaultString(content.getText()));
        entityVO.setContentExt(content);
    }

    /**
     * Convert bo list to vo list.
     *
     * @param entityBOList entity business object list
     * @return converted value
     */
    List<MessageVO> buildVOListByBOList(List<MessageBO> entityBOList);

    /**
     * Convert do page to bo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default Page<MessageBO> buildBOPageByDOPage(Page<MessageDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildBOByDO);
    }

    /**
     * Convert bo page to vo page.
     *
     * @param entityPageBO business object
     * @return converted value
     */
    default Page<MessageVO> buildVOPageByBOPage(Page<MessageBO> entityPageBO) {
        return PageUtil.copyPage(entityPageBO, this::buildVOByBO);
    }

}
