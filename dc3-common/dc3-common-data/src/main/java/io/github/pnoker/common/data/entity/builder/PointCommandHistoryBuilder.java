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

import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import org.mapstruct.Mapper;

/**
 * MapStruct builder converting between point command history DO and VO.
 * <p>
 * The DO and VO share the same enum types for {@code type}/{@code status}/{@code source};
 * the enum index is persisted via {@code @EnumValue} and exposed over JSON by name.
 *
 * @author pnoker
 * @since 2026.6.5
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface PointCommandHistoryBuilder {

    /**
     * Convert do to vo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    PointCommandHistoryVO buildVOByDO(PointCommandHistoryDO entityDO);

    /**
     * Convert do list to vo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<PointCommandHistoryVO> buildVOListByDOList(List<PointCommandHistoryDO> entityDOList);
}
