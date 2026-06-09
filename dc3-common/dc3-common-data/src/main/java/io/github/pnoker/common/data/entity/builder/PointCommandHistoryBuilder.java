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
import io.github.pnoker.common.data.entity.model.PointCommandHistoryDO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.common.enums.PointCommandStatusEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.Objects;

/**
 * MapStruct builder converting between point command history DO and VO.
 * <p>
 * The DO stores {@code status} as its lowercase code string (database shape);
 * the VO exposes the {@link PointCommandStatusEnum}. The code/enum conversion is
 * centralized here (DO -> VO via {@code ofCode}).
 *
 * @author pnoker
 * @version 2026.6.5
 * @since 2026.6.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface PointCommandHistoryBuilder {

    @Mapping(target = "status", ignore = true)
    PointCommandHistoryVO buildVOByDO(PointCommandHistoryDO entityDO);

    @AfterMapping
    default void afterProcess(PointCommandHistoryDO entityDO, @MappingTarget PointCommandHistoryVO entityVO) {
        if (Objects.nonNull(entityDO.getStatus())) {
            entityVO.setStatus(PointCommandStatusEnum.ofCode(entityDO.getStatus()));
        }
    }

    List<PointCommandHistoryVO> buildVOListByDOList(List<PointCommandHistoryDO> entityDOList);

    default Page<PointCommandHistoryVO> buildVOPageByDOPage(Page<PointCommandHistoryDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildVOByDO);
    }

}
