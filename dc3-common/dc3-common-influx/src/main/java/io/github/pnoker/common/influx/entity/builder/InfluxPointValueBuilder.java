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

package io.github.pnoker.common.influx.entity.builder;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.influx.entity.model.InfluxPointValueDO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * PointValue Builder
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface InfluxPointValueBuilder {

    /**
     * BO to Influx DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    InfluxPointValueDO buildMgDOByBO(PointValueBO entityBO);

    /**
     * BOList to Influx DOList
     *
     * @param entityBOList EntityBO List
     * @return EntityDO
     */
    List<InfluxPointValueDO> buildMgDOListByBOList(List<PointValueBO> entityBOList);

    /**
     * Influx DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    PointValueBO buildBOByMgDO(InfluxPointValueDO entityDO);

    /**
     * Influx DOList to BOList
     *
     * @param entityDOList EntityDO 集合
     * @return EntityBO 集合
     */
    List<PointValueBO> buildBOListByDOList(List<InfluxPointValueDO> entityDOList);

}