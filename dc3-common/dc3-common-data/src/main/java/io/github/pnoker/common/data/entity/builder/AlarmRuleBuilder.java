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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.bo.AlarmRuleBO;
import io.github.pnoker.common.data.entity.model.AlarmRuleDO;
import io.github.pnoker.common.data.entity.vo.AlarmRuleVO;
import io.github.pnoker.common.entity.ext.AlarmRuleExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmTypeFlagEnum;
import io.github.pnoker.common.enums.EnableFlagEnum;
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
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface AlarmRuleBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    AlarmRuleBO buildBOByVO(AlarmRuleVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<AlarmRuleBO> buildBOListByVOList(List<AlarmRuleVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "alarmRuleExt", ignore = true)
    @Mapping(target = "alarmTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AlarmRuleDO buildDOByBO(AlarmRuleBO entityBO);

    @AfterMapping
    default void afterProcess(AlarmRuleBO entityBO, @MappingTarget AlarmRuleDO entityDO) {
        // Json Ext
        AlarmRuleExt entityExt = entityBO.getAlarmRuleExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setAlarmRuleExt(ext);

        // AlarmType Flag
        AlarmTypeFlagEnum alarmTypeFlag = entityBO.getAlarmTypeFlag();
        Optional.ofNullable(alarmTypeFlag).ifPresent(value -> entityDO.setAlarmTypeFlag(value.getIndex()));

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
    List<AlarmRuleDO> buildDOListByBOList(List<AlarmRuleBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "alarmRuleExt", ignore = true)
    @Mapping(target = "alarmTypeFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    AlarmRuleBO buildBOByDO(AlarmRuleDO entityDO);

    @AfterMapping
    default void afterProcess(AlarmRuleDO entityDO, @MappingTarget AlarmRuleBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getAlarmRuleExt();
        if (Objects.nonNull(entityExt)) {
            AlarmRuleExt ext = new AlarmRuleExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), AlarmRuleExt.Content.class));
            entityBO.setAlarmRuleExt(ext);
        }

        // AlarmType Flag
        Byte alarmTypeFlag = entityDO.getAlarmTypeFlag();
        entityBO.setAlarmTypeFlag(AlarmTypeFlagEnum.ofIndex(alarmTypeFlag));

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
    List<AlarmRuleBO> buildBOListByDOList(List<AlarmRuleDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    AlarmRuleVO buildVOByBO(AlarmRuleBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<AlarmRuleVO> buildVOListByBOList(List<AlarmRuleBO> entityBOList);

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
    Page<AlarmRuleBO> buildBOPageByDOPage(Page<AlarmRuleDO> entityPageDO);

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
    Page<AlarmRuleVO> buildVOPageByBOPage(Page<AlarmRuleBO> entityPageBO);
}