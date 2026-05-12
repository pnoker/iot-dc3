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

package io.github.pnoker.common.facade.local.builder;

import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * FacadePoint ↔ manager PointBO/Query mapper.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2026.5.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface FacadePointBuilder {

    @Mapping(target = "groupId", ignore = true)
    @Mapping(target = "labelId", ignore = true)
    PointQuery toManagerQuery(FacadePointQuery facadeQuery);

    FacadePointBO toFacadeBO(PointBO managerBO);

}
