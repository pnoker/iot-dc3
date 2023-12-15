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
 * Role Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface RoleBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     *//*
    RoleBO buildBOByVO(RoleVO entityVO);

    *//**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     *//*
    List<RoleBO> buildBOListByVOList(List<RoleVO> entityVOList);

    *//**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     *//*
    @Mapping(target = "deleted", ignore = true)
    RoleDO buildDOByBO(RoleBO entityBO);

    *//**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     *//*
    List<RoleDO> buildDOListByBOList(List<RoleBO> entityBOList);

    *//**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     *//*
    RoleBO buildBOByDO(RoleDO entityDO);

    *//**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     *//*
    List<RoleBO> buildBOListByDOList(List<RoleDO> entityDOList);

    *//**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     *//*
    RoleVO buildVOByBO(RoleBO entityBO);

    *//**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     *//*
    List<RoleVO> buildVOListByBOList(List<RoleBO> entityBOList);

    *//**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     *//*
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<RoleBO> buildBOPageByDOPage(Page<RoleDO> entityPageDO);

    *//**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     *//*
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<RoleVO> buildVOPageByBOPage(Page<RoleBO> entityPageBO);*/
}