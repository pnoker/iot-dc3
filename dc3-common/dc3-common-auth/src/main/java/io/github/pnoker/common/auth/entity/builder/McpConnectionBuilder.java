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

import io.github.pnoker.common.auth.entity.oauth.McpConnectionRecord;
import io.github.pnoker.common.auth.entity.vo.McpConnectionAddVO;
import io.github.pnoker.common.auth.entity.vo.McpConnectionVO;
import io.github.pnoker.common.enums.OAuthGrantTypeEnum;
import io.github.pnoker.common.enums.PrincipalTypeEnum;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct builder converting between MCP connection projection and view objects.
 *
 * @author pnoker
 * @version 2026.6.19
 * @since 2026.6.19
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface McpConnectionBuilder {

    /**
     * Record to VO
     *
     * @param entityRecord EntityRecord
     * @return EntityVO
     */
    @Mapping(target = "principalType", ignore = true)
    @Mapping(target = "grantType", ignore = true)
    McpConnectionVO buildVOByRecord(McpConnectionRecord entityRecord);

    @AfterMapping
    default void afterProcess(McpConnectionRecord entityRecord, @MappingTarget McpConnectionVO entityVO) {
        entityVO.setPrincipalType(PrincipalTypeEnum.ofValue(entityRecord.getPrincipalType()));
        entityVO.setGrantType(OAuthGrantTypeEnum.ofValue(entityRecord.getGrantType()));
    }

    /**
     * RecordList to VOList
     *
     * @param entityRecordList EntityRecord Array
     * @return EntityVO Array
     */
    List<McpConnectionVO> buildVOListByRecordList(List<McpConnectionRecord> entityRecordList);

    /**
     * AddVO to Record
     *
     * @param entityVO Create request VO
     * @return EntityRecord
     */
    McpConnectionRecord buildRecordByAddVO(McpConnectionAddVO entityVO);

}
