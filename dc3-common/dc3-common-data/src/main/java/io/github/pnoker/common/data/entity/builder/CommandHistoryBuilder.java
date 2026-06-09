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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.model.CommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.CommandHistoryVO;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * MapStruct builder converting between command history DO and VO.
 * <p>
 * The DO and VO share the same enum types for {@code status}/{@code source}; the
 * enum index is persisted via {@code @EnumValue} and exposed over JSON by name.
 *
 * @author pnoker
 * @version 2026.6.5
 * @since 2026.6.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface CommandHistoryBuilder {

    CommandHistoryVO buildVOByDO(CommandHistoryDO entityDO);

    List<CommandHistoryVO> buildVOListByDOList(List<CommandHistoryDO> entityDOList);

    default Page<CommandHistoryVO> buildVOPageByDOPage(Page<CommandHistoryDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildVOByDO);
    }

}
