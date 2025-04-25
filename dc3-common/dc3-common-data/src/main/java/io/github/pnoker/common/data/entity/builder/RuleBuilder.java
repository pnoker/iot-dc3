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
import io.github.pnoker.common.data.entity.bo.RuleBO;
import io.github.pnoker.common.data.entity.model.RuleDO;
import io.github.pnoker.common.data.entity.vo.RuleVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.RuleExt;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
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
 * AlarmRule Builder
 *
 * @author pnoker
 * @version 2025.2.5
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RuleBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    RuleBO buildBOByVO(RuleVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<RuleBO> buildBOListByVOList(List<RuleVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "ruleExt", ignore = true)
    @Mapping(target = "entityTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    RuleDO buildDOByBO(RuleBO entityBO);

    @AfterMapping
    default void afterProcess(RuleBO entityBO, @MappingTarget RuleDO entityDO) {
        // Code
        if (CharSequenceUtil.isEmpty(entityBO.getRuleCode())) {
            entityDO.setRuleCode(CodeUtil.getCode());
        }

// Json Ext
        RuleExt entityExt = entityBO.getRuleExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setRuleExt(ext);

        // AlarmType Flag
        AlarmTypeFlagEnum alarmTypeFlag = entityBO.getEntityTypeFlag();
        Optional.ofNullable(alarmTypeFlag).ifPresent(value -> entityDO.setEntityTypeFlag(value.getIndex()));

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
    List<RuleDO> buildDOListByBOList(List<RuleBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "ruleExt", ignore = true)
    @Mapping(target = "entityTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    RuleBO buildBOByDO(RuleDO entityDO);

    @AfterMapping
    default void afterProcess(RuleDO entityDO, @MappingTarget RuleBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getRuleExt();
        if (Objects.nonNull(entityExt)) {
            RuleExt ext = new RuleExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), RuleExt.Content.class));
            entityBO.setRuleExt(ext);
        }

        // AlarmType Flag
        Byte alarmTypeFlag = entityDO.getEntityTypeFlag();
        entityBO.setEntityTypeFlag(AlarmTypeFlagEnum.ofIndex(alarmTypeFlag));

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
    List<RuleBO> buildBOListByDOList(List<RuleDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    RuleVO buildVOByBO(RuleBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<RuleVO> buildVOListByBOList(List<RuleBO> entityBOList);

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
    Page<RuleBO> buildBOPageByDOPage(Page<RuleDO> entityPageDO);

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
    Page<RuleVO> buildVOPageByBOPage(Page<RuleBO> entityPageBO);
}