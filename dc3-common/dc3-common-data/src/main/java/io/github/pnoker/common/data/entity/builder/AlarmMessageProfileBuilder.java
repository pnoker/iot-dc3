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
import io.github.pnoker.common.data.entity.bo.AlarmMessageProfileBO;
import io.github.pnoker.common.data.entity.model.AlarmMessageProfileDO;
import io.github.pnoker.common.data.entity.vo.AlarmMessageProfileVO;
import io.github.pnoker.common.entity.ext.AlarmMessageExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AlarmMessageLevelFlagEnum;
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
 * AlarmMessageProfile Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface AlarmMessageProfileBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    AlarmMessageProfileBO buildBOByVO(AlarmMessageProfileVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<AlarmMessageProfileBO> buildBOListByVOList(List<AlarmMessageProfileVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "alarmMessageExt", ignore = true)
    @Mapping(target = "alarmMessageLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AlarmMessageProfileDO buildDOByBO(AlarmMessageProfileBO entityBO);

    @AfterMapping
    default void afterProcess(AlarmMessageProfileBO entityBO, @MappingTarget AlarmMessageProfileDO entityDO) {
        // Json Ext
        AlarmMessageExt entityExt = entityBO.getAlarmMessageExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setAlarmMessageExt(ext);

        // AlarmMessageLevel Flag
        AlarmMessageLevelFlagEnum alarmMessageLevel = entityBO.getAlarmMessageLevel();
        Optional.ofNullable(alarmMessageLevel).ifPresent(value -> entityDO.setAlarmMessageLevel(value.getIndex()));

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
    List<AlarmMessageProfileDO> buildDOListByBOList(List<AlarmMessageProfileBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "alarmMessageExt", ignore = true)
    @Mapping(target = "alarmMessageLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    AlarmMessageProfileBO buildBOByDO(AlarmMessageProfileDO entityDO);

    @AfterMapping
    default void afterProcess(AlarmMessageProfileDO entityDO, @MappingTarget AlarmMessageProfileBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getAlarmMessageExt();
        if (Objects.nonNull(entityExt)) {
            AlarmMessageExt ext = new AlarmMessageExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), AlarmMessageExt.Content.class));
            entityBO.setAlarmMessageExt(ext);
        }

        // AlarmMessageLevel Flag
        Byte alarmMessageLevel = entityDO.getAlarmMessageLevel();
        entityBO.setAlarmMessageLevel(AlarmMessageLevelFlagEnum.ofIndex(alarmMessageLevel));

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
    List<AlarmMessageProfileBO> buildBOListByDOList(List<AlarmMessageProfileDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    AlarmMessageProfileVO buildVOByBO(AlarmMessageProfileBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<AlarmMessageProfileVO> buildVOListByBOList(List<AlarmMessageProfileBO> entityBOList);

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
    Page<AlarmMessageProfileBO> buildBOPageByDOPage(Page<AlarmMessageProfileDO> entityPageDO);

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
    Page<AlarmMessageProfileVO> buildVOPageByBOPage(Page<AlarmMessageProfileBO> entityPageBO);
}