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
package io.github.pnoker.common.agentic.entity.builder;

import io.github.pnoker.common.agentic.entity.bo.AttachmentBO;
import io.github.pnoker.common.agentic.entity.vo.AttachmentVO;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Maps reactive attachment projections to API resources. */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface AttachmentBuilder {

    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    AttachmentBO buildBOByVO(AttachmentVO entityVO);

    List<AttachmentBO> buildBOListByVOList(List<AttachmentVO> entityVOList);

    AttachmentVO buildVOByBO(AttachmentBO entityBO);

    List<AttachmentVO> buildVOListByBOList(List<AttachmentBO> entityBOList);
}
