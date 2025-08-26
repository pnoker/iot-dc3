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

package io.github.pnoker.common.auth.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.UserLoginBO;
import io.github.pnoker.common.auth.entity.model.UserLoginDO;
import io.github.pnoker.common.auth.entity.vo.UserLoginVO;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Optional;

/**
 * UserLogin Builder
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface UserLoginBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    UserLoginBO buildBOByVO(UserLoginVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO 集合
     * @return EntityBO 集合
     */
    List<UserLoginBO> buildBOListByVOList(List<UserLoginVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    UserLoginDO buildDOByBO(UserLoginBO entityBO);

    @AfterMapping
    default void afterProcess(UserLoginBO entityBO, @MappingTarget UserLoginDO entityDO) {
        // Enable Flag
        EnableFlagEnum enableFlag = entityBO.getEnableFlag();
        Optional.ofNullable(enableFlag).ifPresent(value -> entityDO.setEnableFlag(value.getIndex()));
    }

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO 集合
     * @return EntityDO 集合
     */
    List<UserLoginDO> buildDOListByBOList(List<UserLoginBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "enableFlag", ignore = true)
    UserLoginBO buildBOByDO(UserLoginDO entityDO);

    @AfterMapping
    default void afterProcess(UserLoginDO entityDO, @MappingTarget UserLoginBO entityBO) {
        // Enable Flag
        Byte enableFlag = entityDO.getEnableFlag();
        entityBO.setEnableFlag(EnableFlagEnum.ofIndex(enableFlag));
    }

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO 集合
     * @return EntityBO 集合
     */
    List<UserLoginBO> buildBOListByDOList(List<UserLoginDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    UserLoginVO buildVOByBO(UserLoginBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO 集合
     * @return EntityVO Array
     */
    List<UserLoginVO> buildVOListByBOList(List<UserLoginBO> entityBOList);

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
    Page<UserLoginBO> buildBOPageByDOPage(Page<UserLoginDO> entityPageDO);

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
    Page<UserLoginVO> buildVOPageByBOPage(Page<UserLoginBO> entityPageBO);
}