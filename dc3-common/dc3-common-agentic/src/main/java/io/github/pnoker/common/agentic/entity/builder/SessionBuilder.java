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

import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.utils.MapStructUtil;
import java.util.List;
import org.mapstruct.Mapper;

/** Maps reactive session projections to API resources. */
@Mapper(
        componentModel = "spring",
        uses = {MapStructUtil.class})
public interface SessionBuilder {

    /** Convert the business object into its value-object form. */
    SessionVO buildVOByBO(SessionBO entityBO);

    /** Convert the business objects into their value-object forms. */
    List<SessionVO> buildVOListByBOList(List<SessionBO> entityBOList);
}
