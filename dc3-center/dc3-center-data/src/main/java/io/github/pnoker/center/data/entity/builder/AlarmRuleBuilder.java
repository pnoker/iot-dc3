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

package io.github.pnoker.center.data.entity.builder;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.data.entity.bo.AlarmRuleBO;
import io.github.pnoker.center.data.entity.model.AlarmRuleDO;
import io.github.pnoker.center.data.entity.vo.AlarmRuleVO;
import io.github.pnoker.common.entity.ext.AlarmRuleExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.utils.JsonUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * AlarmRule Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
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
    @Mapping(target = "deleted", ignore = true)
    AlarmRuleDO buildDOByBO(AlarmRuleBO entityBO);

    @AfterMapping
    default void afterProcess(AlarmRuleBO entityBO, @MappingTarget AlarmRuleDO entityDO) {
        AlarmRuleExt entityExt = entityBO.getAlarmRuleExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            JsonExt.JsonExtBuilder<?, ?> builder = JsonExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.toJsonString(entityExt.getContent()));
            entityDO.setAlarmRuleExt(builder.build());
        }
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
    AlarmRuleBO buildBOByDO(AlarmRuleDO entityDO);

    @AfterMapping
    default void afterProcess(AlarmRuleDO entityDO, @MappingTarget AlarmRuleBO entityBO) {
        JsonExt entityExt = entityDO.getAlarmRuleExt();
        if (ObjectUtil.isNotNull(entityExt)) {
            AlarmRuleExt.AlarmRuleExtBuilder<?, ?> builder = AlarmRuleExt.builder();
            builder.type(entityExt.getType()).version(entityExt.getVersion()).remark(entityExt.getRemark());
            builder.content(JsonUtil.parseObject(entityExt.getContent(), AlarmRuleExt.Content.class));
            entityBO.setAlarmRuleExt(builder.build());
        }
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
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<AlarmRuleBO> buildBOPageByDOPage(Page<AlarmRuleDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<AlarmRuleVO> buildVOPageByBOPage(Page<AlarmRuleBO> entityPageBO);
}