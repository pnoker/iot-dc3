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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.entity.bo.UserBO;
import io.github.pnoker.center.auth.entity.model.UserDO;
import io.github.pnoker.center.auth.entity.vo.UserVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * User Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface UserBuilder {

    /**
     * VO to BO
     *
     * @param entityVO EntityVO
     * @return EntityBO
     */
    UserBO buildBOByVO(UserVO entityVO);

    /**
     * VOList to BOList
     *
     * @param entityVOList EntityVO Array
     * @return EntityBO Array
     */
    List<UserBO> buildBOListByVOList(List<UserVO> entityVOList);

    /**
     * BO to DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    @Mapping(target = "socialExt", ignore = true)
    @Mapping(target = "identityExt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    UserDO buildDOByBO(UserBO entityBO);

    /**
     * BOList to DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO Array
     */
    List<UserDO> buildDOListByBOList(List<UserBO> entityBOList);

    /**
     * DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    UserBO buildBOByDO(UserDO entityDO);

    /**
     * DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<UserBO> buildBOListByDOList(List<UserDO> entityDOList);

    /**
     * BO to VO
     *
     * @param entityBO EntityBO
     * @return EntityVO
     */
    UserVO buildVOByBO(UserBO entityBO);

    /**
     * BOList to VOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityVO Array
     */
    List<UserVO> buildVOListByBOList(List<UserBO> entityBOList);

    /**
     * DOPage to BOPage
     *
     * @param entityPageDO EntityDO Page
     * @return EntityBO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<UserBO> buildBOPageByDOPage(Page<UserDO> entityPageDO);

    /**
     * BOPage to VOPage
     *
     * @param entityPageBO EntityBO Page
     * @return EntityVO Page
     */
    @Mapping(target = "optimizeJoinOfCountSql", ignore = true)
    Page<UserVO> buildVOPageByBOPage(Page<UserBO> entityPageBO);
}