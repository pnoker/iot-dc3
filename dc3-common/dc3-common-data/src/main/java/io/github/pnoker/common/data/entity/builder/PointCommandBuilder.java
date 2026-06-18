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

package io.github.pnoker.common.data.entity.builder;

import io.github.pnoker.common.data.entity.bo.PointCommandReadBO;
import io.github.pnoker.common.data.entity.bo.PointCommandWriteBO;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

/**
 * MapStruct builder converting point command request VO to BO.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PointCommandBuilder {

    /**
     * Read command VO to BO
     *
     * @param entityVO PointCommandReadVO
     * @return PointCommandReadBO
     */
    PointCommandReadBO buildBOByVO(PointCommandReadVO entityVO);

    /**
     * Write command VO to BO
     *
     * @param entityVO PointCommandWriteVO
     * @return PointCommandWriteBO
     */
    PointCommandWriteBO buildBOByVO(PointCommandWriteVO entityVO);

}
