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

package io.github.pnoker.common.auth.entity.builder;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.auth.entity.bo.MenuBO;
import io.github.pnoker.common.auth.entity.model.MenuDO;
import io.github.pnoker.common.auth.entity.vo.MenuVO;
import io.github.pnoker.common.entity.ext.JsonExt;
import io.github.pnoker.common.entity.ext.MenuExt;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.enums.MenuLevelFlagEnum;
import io.github.pnoker.common.enums.MenuTypeFlagEnum;
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
 * Menu Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface MenuBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    @Mapping(target = "tenantId", ignore = true)
    MenuBO buildBOByVO(MenuVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<MenuBO> buildBOListByVOList(List<MenuVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "menuExt", ignore = true)
    @Mapping(target = "menuTypeFlag", ignore = true)
    @Mapping(target = "menuLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    MenuDO buildDOByBO(MenuBO entityBO);

    @AfterMapping
    default void afterProcess(MenuBO entityBO, @MappingTarget MenuDO entityDO) {
        // Json Ext
        MenuExt entityExt = entityBO.getMenuExt();
        JsonExt ext = new JsonExt();
        if (Objects.nonNull(entityExt)) {
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.toJsonString(entityExt.getContent()));
        }
        entityDO.setMenuExt(ext);

        // MenuType Flag
        MenuTypeFlagEnum menuTypeFlag = entityBO.getMenuTypeFlag();
        Optional.ofNullable(menuTypeFlag).ifPresent(value -> entityDO.setMenuTypeFlag(value.getIndex()));

        // MenuLevel Flag
        MenuLevelFlagEnum menuLevel = entityBO.getMenuLevel();
        Optional.ofNullable(menuLevel).ifPresent(value -> entityDO.setMenuLevel(value.getIndex()));

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
    List<MenuDO> buildDOListByBOList(List<MenuBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "menuExt", ignore = true)
    @Mapping(target = "menuTypeFlag", ignore = true)
    @Mapping(target = "menuLevel", ignore = true)
    @Mapping(target = "enableFlag", ignore = true)
    MenuBO buildBOByDO(MenuDO entityDO);

    @AfterMapping
    default void afterProcess(MenuDO entityDO, @MappingTarget MenuBO entityBO) {
        // Json Ext
        JsonExt entityExt = entityDO.getMenuExt();
        if (Objects.nonNull(entityExt)) {
            MenuExt ext = new MenuExt();
            ext.setType(entityExt.getType());
            ext.setVersion(entityExt.getVersion());
            ext.setRemark(entityExt.getRemark());
            ext.setContent(JsonUtil.parseObject(entityExt.getContent(), MenuExt.Content.class));
            entityBO.setMenuExt(ext);
        }

        // MenuType Flag
        Byte menuTypeFlag = entityDO.getMenuTypeFlag();
        entityBO.setMenuTypeFlag(MenuTypeFlagEnum.ofIndex(menuTypeFlag));

        // MenuLevel Flag
        Byte menuLevel = entityDO.getMenuLevel();
        entityBO.setMenuLevel(MenuLevelFlagEnum.ofIndex(menuLevel));

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
    List<MenuBO> buildBOListByDOList(List<MenuDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    MenuVO buildVOByBO(MenuBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<MenuVO> buildVOListByBOList(List<MenuBO> entityBOList);

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
    Page<MenuBO> buildBOPageByDOPage(Page<MenuDO> entityPageDO);

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
    Page<MenuVO> buildVOPageByBOPage(Page<MenuBO> entityPageBO);
}