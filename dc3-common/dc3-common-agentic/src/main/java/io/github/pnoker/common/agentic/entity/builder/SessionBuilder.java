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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.agentic.entity.bo.SessionBO;
import io.github.pnoker.common.agentic.entity.model.SessionDO;
import io.github.pnoker.common.agentic.entity.vo.SessionVO;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = { MapStructUtil.class })
public interface SessionBuilder {

	SessionBO buildBOByVO(SessionVO entityVO);

	List<SessionBO> buildBOListByVOList(List<SessionVO> entityVOList);

	SessionVO buildVOByBO(SessionBO entityBO);

	List<SessionVO> buildVOListByBOList(List<SessionBO> entityBOList);

	SessionBO buildBOByDO(SessionDO entityDO);

	List<SessionBO> buildBOListByDOList(List<SessionDO> entityDOList);

	@Mapping(target = "deleted", ignore = true)
	SessionDO buildDOByBO(SessionBO entityBO);

	List<SessionDO> buildDOListByBOList(List<SessionBO> entityBOList);

	@Mapping(target = "orders", ignore = true)
	@Mapping(target = "countId", ignore = true)
	@Mapping(target = "maxLimit", ignore = true)
	@Mapping(target = "searchCount", ignore = true)
	@Mapping(target = "optimizeCountSql", ignore = true)
	@Mapping(target = "optimizeJoinOfCountSql", ignore = true)
	Page<SessionVO> buildVOPageByBOPage(Page<SessionBO> entityPageBO);

	@Mapping(target = "orders", ignore = true)
	@Mapping(target = "countId", ignore = true)
	@Mapping(target = "maxLimit", ignore = true)
	@Mapping(target = "searchCount", ignore = true)
	@Mapping(target = "optimizeCountSql", ignore = true)
	@Mapping(target = "optimizeJoinOfCountSql", ignore = true)
	Page<SessionBO> buildBOPageByDOPage(Page<SessionDO> entityPageDO);

}
