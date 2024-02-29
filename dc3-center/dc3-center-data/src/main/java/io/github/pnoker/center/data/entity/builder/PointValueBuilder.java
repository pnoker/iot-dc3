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

import io.github.pnoker.center.data.entity.bo.PointValueBO;
import io.github.pnoker.center.data.entity.point.MgPointValueDO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * PointValue Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring")
public interface PointValueBuilder {

    /**
     * BO to Mongo DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    MgPointValueDO buildMgDOByBO(PointValueBO entityBO);

    /**
     * Mongo DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    @Mapping(target = "children", ignore = true)
    PointValueBO buildBOByMgDO(MgPointValueDO entityDO);

    /**
     * Mongo DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<PointValueBO> buildBOListByDOList(List<MgPointValueDO> entityDOList);

}