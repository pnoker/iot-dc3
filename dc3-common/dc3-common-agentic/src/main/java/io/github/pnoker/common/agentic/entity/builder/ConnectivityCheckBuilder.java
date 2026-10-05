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

import io.github.pnoker.common.agentic.entity.model.ConnectivityCheckResult;
import io.github.pnoker.common.agentic.entity.model.ConnectivityLevelResult;
import io.github.pnoker.common.agentic.entity.vo.CheckLevelResultVO;
import io.github.pnoker.common.agentic.entity.vo.ProviderCheckResultVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** MapStruct builder converting connectivity check internal results into API value objects. */
@Mapper(componentModel = "spring")
public interface ConnectivityCheckBuilder {

    /** Convert one probe-level outcome into its value-object form. */
    @Mapping(target = "errorType", source = "errorType")
    CheckLevelResultVO buildLevelVOByResult(ConnectivityLevelResult result);

    /** Convert the aggregate check outcome into its value-object form. */
    ProviderCheckResultVO buildVOByResult(ConnectivityCheckResult result);
}
