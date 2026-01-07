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
 * Redis Point Value Builder Interface
 * <p>
 * MapStruct mapper interface for converting between PointValueBO
 * and RedisPointValueDO objects. Provides bidirectional conversion
 * for single objects and collections.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface RedisPointValueBuilder {

    /**
     * Convert PointValueBO to RedisPointValueDO
     *
     * @param entityBO Point value business object to convert
     * @return Redis point value data object
     */
    RedisPointValueDO buildDOByBO(PointValueBO entityBO);

    /**
     * Convert list of PointValueBO to list of RedisPointValueDO
     *
     * @param entityBOList List of point value business objects to convert
     * @return List of Redis point value data objects
     */
    List<RedisPointValueDO> buildDOListByBOList(List<PointValueBO> entityBOList);

    /**
     * Convert RedisPointValueDO to PointValueBO
     *
     * @param entityDO Redis point value data object to convert
     * @return Point value business object
     */
    PointValueBO buildBOByDO(RedisPointValueDO entityDO);

    /**
     * Convert list of RedisPointValueDO to list of PointValueBO
     *
     * @param entityDOList List of Redis point value data objects to convert
     * @return List of point value business objects
     */
    List<PointValueBO> buildBOListByDOList(List<RedisPointValueDO> entityDOList);

}