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

import io.github.pnoker.common.data.entity.bo.CommandCallBO;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct builder converting between command history DO and VO.
 * <p>
 * The DO and VO share the same enum types for {@code status}/{@code source}; the
 * enum index is persisted via {@code @EnumValue} and exposed over JSON by name.
 *
 * @author pnoker
 * @since 2026.6.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface CommandHistoryBuilder {

    /**
     * Command call request VO to BO
     *
     * @param entityVO CommandCallVO
     * @return CommandCallBO
     */
    @Mapping(target = "source", ignore = true)
    @Mapping(target = "sourceUserId", ignore = true)
    CommandCallBO buildBOByVO(CommandCallVO entityVO);

    /**
     * Convert do to vo.
     *
     * @param entityDO persistence object
     * @return converted value
     */
    CommandHistoryVO buildVOByDO(CommandHistoryDO entityDO);

    /**
     * Convert do list to vo list.
     *
     * @param entityDOList entity persistence object list
     * @return converted value
     */
    List<CommandHistoryVO> buildVOListByDOList(List<CommandHistoryDO> entityDOList);

    /**
     * Convert do page to vo page.
     *
     * @param entityPageDO persistence object
     * @return converted value
     */
    default OffsetPage<CommandHistoryVO> buildVOPageByDOPage(OffsetPage<CommandHistoryDO> entityPageDO) {
        return OffsetPage.of(entityPageDO.items().stream().map(this::buildVOByDO).toList(),
                entityPageDO.offset(), entityPageDO.limit(), entityPageDO.total());
    }

}
