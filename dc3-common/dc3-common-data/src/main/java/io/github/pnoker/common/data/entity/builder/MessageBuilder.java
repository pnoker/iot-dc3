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

package io.github.pnoker.common.data.entity.builder;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.vo.MessageVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.MessageExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.CodeUtil;
import io.github.pnoker.common.utils.JsonUtil;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * AlarmMessageProfile Builder
 *
 * @author pnoker
 * @version 2025.2.1
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface MessageBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    MessageBO buildBOByVO(MessageVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<MessageBO> buildBOListByVOList(List<MessageVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "messageExt", ignore = true)
    @Mapping(target = "messageLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    MessageDO buildDOByBO(MessageBO entityBO);

    @AfterMapping
    default void afterProcess(MessageBO entityBO, @MappingTarget MessageDO entityDO) {
        // Code
        if (CharSequenceUtil.isEmpty(entityBO.getMessageCode())) {
            entityDO.setMessageCode(CodeUtil.getCode());
        }

// Json Ext
        MessageExt entityExt = entityBO.getMessageExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setMessageExt(ext);

        // AlarmMessageLevel Flag
        AlarmMessageLevelFlagEnum alarmMessageLevel = entityBO.getMessageLevel();
        Optional.ofNullable(alarmMessageLevel).ifPresent(value -> entityDO.setMessageLevel(value.getIndex()));

        // Enable Flag
        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<MessageDO> buildDOListByBOList(List<MessageBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "messageExt", ignore = true)
    @Mapping(target = "messageLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    MessageBO buildBOByDO(MessageDO entityDO);

    @AfterMapping
    default void afterProcess(MessageDO entityDO, @MappingTarget MessageBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getMessageExt();
        if (Objects.nonNull(entityExt)) {
            MessageExt ext = new MessageExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), MessageExt.Content.class));
            entityBO.setMessageExt(ext);
        }

        // AlarmMessageLevel Flag
        Byte alarmMessageLevel = entityDO.getMessageLevel();
        entityBO.setMessageLevel(AlarmMessageLevelFlagEnum.ofIndex(alarmMessageLevel));

        // Enable Flag
        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<MessageBO> buildBOListByDOList(List<MessageDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    MessageVO buildVOByBO(MessageBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<MessageVO> buildVOListByBOList(List<MessageBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<MessageBO> buildBOPageByDOPage(Page<MessageDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "orders", ignore = true)
    @Mapping(target = "countId", ignore = true)
    @Mapping(target = "maxLimit", ignore = true)
    @Mapping(target = "searchCount", ignore = true)
    @Mapping(target = "optimizeCountSql", ignore = true)
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<MessageVO> buildVOPageByBOPage(Page<MessageBO> entityPageBO);
}