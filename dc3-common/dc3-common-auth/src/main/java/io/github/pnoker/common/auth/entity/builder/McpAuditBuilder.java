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

import io.github.pnoker.common.auth.entity.oauth.McpAuditCommand;
import io.github.pnoker.common.auth.entity.vo.McpAuditVO;
import io.github.pnoker.common.enums.McpAuditStatusEnum;
import io.github.pnoker.common.enums.McpRiskLevelEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct builder converting an MCP audit command projection to its view object.
 *
 * @author pnoker
 * @since 2026.6.19
 */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface McpAuditBuilder {

    /**
     * Record to VO
     *
     * @param entityRecord EntityRecord
     * @return EntityVO
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "riskLevel", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "remark", ignore = true)
    @Mapping(target = "creatorId", ignore = true)
    @Mapping(target = "creatorName", ignore = true)
    @Mapping(target = "operatorId", ignore = true)
    @Mapping(target = "operatorName", ignore = true)
    @Mapping(target = "operateTime", ignore = true)
    McpAuditVO buildVOByRecord(McpAuditCommand entityRecord);

    /**
     * After process.
     *
     * @param entityRecord entity record
     * @param entityVO     view object
     */
    @AfterMapping
    default void afterProcess(McpAuditCommand entityRecord, @MappingTarget McpAuditVO entityVO) {
        entityVO.setPrincipalType(PrincipalTypeEnum.ofValue(entityRecord.getPrincipalType()));
        entityVO.setRiskLevel(McpRiskLevelEnum.ofValue(entityRecord.getRiskLevel()));
        entityVO.setStatus(McpAuditStatusEnum.ofValue(entityRecord.getStatus()));
    }

    /**
     * RecordList to VOList
     *
     * @param entityRecordList EntityRecord Array
     * @return EntityVO Array
     */
    List<McpAuditVO> buildVOListByRecordList(List<McpAuditCommand> entityRecordList);
}
