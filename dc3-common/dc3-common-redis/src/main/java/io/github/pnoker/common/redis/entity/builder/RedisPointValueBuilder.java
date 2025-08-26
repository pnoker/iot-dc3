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

package io.github.pnoker.common.redis.entity.builder;

import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.redis.entity.model.RedisPointValueDO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * PointValue Builder
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RedisPointValueBuilder {

    /**
     * BO to Redis DO
     *
     * @param entityBO EntityBO
     * @return EntityDO
     */
    RedisPointValueDO buildDOByBO(PointValueBO entityBO);

    /**
     * BOList to Redis DOList
     *
     * @param entityBOList EntityBO Array
     * @return EntityDO
     */
    List<RedisPointValueDO> buildDOListByBOList(List<PointValueBO> entityBOList);

    /**
     * Redis DO to BO
     *
     * @param entityDO EntityDO
     * @return EntityBO
     */
    PointValueBO buildBOByDO(RedisPointValueDO entityDO);

    /**
     * Redis DOList to BOList
     *
     * @param entityDOList EntityDO Array
     * @return EntityBO Array
     */
    List<PointValueBO> buildBOListByDOList(List<RedisPointValueDO> entityDOList);

}