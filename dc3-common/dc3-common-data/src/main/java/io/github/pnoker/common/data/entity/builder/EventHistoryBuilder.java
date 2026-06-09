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
import io.github.pnoker.common.data.entity.model.EventHistoryDO;
import io.github.pnoker.common.data.entity.vo.EventHistoryVO;
import io.github.pnoker.common.enums.EventHistoryAcknowledgeFlagEnum;
import io.github.pnoker.common.enums.EventLevelEnum;
import io.github.pnoker.common.enums.EventTypeFlagEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import io.github.pnoker.common.utils.PageUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct builder converting between event history DO and VO.
 * <p>
 * The DO stores type/level/acknowledge flags as database-coded {@code Byte}
 * columns; the VO exposes the corresponding domain enums. The enum/index
 * conversion is centralized here (DO -> VO via {@code Enum.ofIndex(...)}) rather
 * than scattered across services.
 *
 * @author pnoker
 * @version 2026.6.5
 * @since 2026.6.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface EventHistoryBuilder {

    @Mapping(target = "eventTypeFlag", ignore = true)
    @Mapping(target = "eventLevelFlag", ignore = true)
    @Mapping(target = "acknowledgeFlag", ignore = true)
    EventHistoryVO buildVOByDO(EventHistoryDO entityDO);

    @AfterMapping
    default void afterProcess(EventHistoryDO entityDO, @MappingTarget EventHistoryVO entityVO) {
        entityVO.setEventTypeFlag(EventTypeFlagEnum.ofIndex(entityDO.getEventTypeFlag()));
        entityVO.setEventLevelFlag(EventLevelEnum.ofIndex(entityDO.getEventLevelFlag()));
        entityVO.setAcknowledgeFlag(EventHistoryAcknowledgeFlagEnum.ofIndex(entityDO.getAcknowledgeFlag()));
    }

    List<EventHistoryVO> buildVOListByDOList(List<EventHistoryDO> entityDOList);

    default Page<EventHistoryVO> buildVOPageByDOPage(Page<EventHistoryDO> entityPageDO) {
        return PageUtil.copyPage(entityPageDO, this::buildVOByDO);
    }

}
