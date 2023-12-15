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

package io.github.pnoker.center.auth.entity.builder;

import org.mapstruct.Mapper;

/**
 * Menu Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface MenuBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     *//*
    MenuBO buildBOByVO(MenuVO entityVO);

    *//**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     *//*
    List<MenuBO> buildBOListByVOList(List<MenuVO> entityVOList);

    *//**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     *//*
    @Mapping(target = "deleted", ignore = true)
    MenuDO buildDOByBO(MenuBO entityBO);

    *//**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     *//*
    List<MenuDO> buildDOListByBOList(List<MenuBO> entityBOList);

    *//**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     *//*
    MenuBO buildBOByDO(MenuDO entityDO);

    *//**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     *//*
    List<MenuBO> buildBOListByDOList(List<MenuDO> entityDOList);

    *//**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     *//*
    MenuVO buildVOByBO(MenuBO entityBO);

    *//**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     *//*
    List<MenuVO> buildVOListByBOList(List<MenuBO> entityBOList);

    *//**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     *//*
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<MenuBO> buildBOPageByDOPage(Page<MenuDO> entityPageDO);

    *//**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     *//*
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<MenuVO> buildVOPageByBOPage(Page<MenuBO> entityPageBO);*/
}