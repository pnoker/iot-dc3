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

package io.github.pnoker.common.auth.entity.builder;

import io.github.pnoker.common.auth.entity.oauth.McpToolRecord;
import io.github.pnoker.common.auth.entity.vo.McpToolVO;
import io.github.pnoker.common.enums.McpRiskLevelEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct builder converting an MCP tool projection to its view object.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface McpToolBuilder {

    /**
     * Record to VO
     *
     * @param entityRecord EntityRecord
     * @return EntityVO
     */
    @Mapping(target = "riskLevel", ignore = true)
    McpToolVO buildVOByRecord(McpToolRecord entityRecord);

    @AfterMapping
    default void afterProcess(McpToolRecord entityRecord, @MappingTarget McpToolVO entityVO) {
        entityVO.setRiskLevel(McpRiskLevelEnum.ofValue(entityRecord.getRiskLevel()));
    }

    /**
     * RecordList to VOList
     *
     * @param entityRecordList EntityRecord Array
     * @return EntityVO Array
     */
    List<McpToolVO> buildVOListByRecordList(List<McpToolRecord> entityRecordList);

}
