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
import io.github.pnoker.common.data.entity.bo.AlarmNotifyProfileBO;
import io.github.pnoker.common.data.entity.model.AlarmNotifyProfileDO;
import io.github.pnoker.common.data.entity.vo.AlarmNotifyProfileVO;
import io.github.pnoker.common.entity.ext.AlarmNotifyExt;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.enums.AutoConfirmFlagEnum;
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
 * AlarmNotifyProfile Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface AlarmNotifyProfileBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    AlarmNotifyProfileBO buildBOByVO(AlarmNotifyProfileVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<AlarmNotifyProfileBO> buildBOListByVOList(List<AlarmNotifyProfileVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "alarmNotifyExt", ignore = true)
    @Mapping(target = "autoConfirmFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AlarmNotifyProfileDO buildDOByBO(AlarmNotifyProfileBO entityBO);

    @AfterMapping
    default void afterProcess(AlarmNotifyProfileBO entityBO, @MappingTarget AlarmNotifyProfileDO entityDO) {
        // Json Ext
        AlarmNotifyExt entityExt = entityBO.getAlarmNotifyExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setAlarmNotifyExt(ext);

        // AutoConfirm Flag
        AutoConfirmFlagEnum autoConfirmFlag = entityBO.getAutoConfirmFlag();
        Optional.ofNullable(autoConfirmFlag).ifPresent(value -> entityDO.setAutoConfirmFlag(value.getIndex()));

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
    List<AlarmNotifyProfileDO> buildDOListByBOList(List<AlarmNotifyProfileBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "alarmNotifyExt", ignore = true)
    @Mapping(target = "autoConfirmFlag", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    AlarmNotifyProfileBO buildBOByDO(AlarmNotifyProfileDO entityDO);

    @AfterMapping
    default void afterProcess(AlarmNotifyProfileDO entityDO, @MappingTarget AlarmNotifyProfileBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getAlarmNotifyExt();
        if (Objects.nonNull(entityExt)) {
            AlarmNotifyExt ext = new AlarmNotifyExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), AlarmNotifyExt.Content.class));
            entityBO.setAlarmNotifyExt(ext);
        }

        // AutoConfirm Flag
        Byte autoConfirmFlag = entityDO.getAutoConfirmFlag();
        entityBO.setAutoConfirmFlag(AutoConfirmFlagEnum.ofIndex(autoConfirmFlag));

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
    List<AlarmNotifyProfileBO> buildBOListByDOList(List<AlarmNotifyProfileDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    AlarmNotifyProfileVO buildVOByBO(AlarmNotifyProfileBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<AlarmNotifyProfileVO> buildVOListByBOList(List<AlarmNotifyProfileBO> entityBOList);

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
    Page<AlarmNotifyProfileBO> buildBOPageByDOPage(Page<AlarmNotifyProfileDO> entityPageDO);

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
    Page<AlarmNotifyProfileVO> buildVOPageByBOPage(Page<AlarmNotifyProfileBO> entityPageBO);
}