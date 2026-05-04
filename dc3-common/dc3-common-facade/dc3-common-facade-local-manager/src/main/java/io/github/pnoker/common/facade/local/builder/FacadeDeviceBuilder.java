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

import io.github.pnoker.common.facade.entity.bo.FacadeDeviceBO;
import io.github.pnoker.common.facade.entity.query.FacadeDeviceQuery;
import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.query.DeviceQuery;
import io.github.pnoker.common.utils.MapStructUtil;
import org.mapstruct.Mapper;

/**
 * Converts between the facade-api shapes and {@code dc3-common-manager} internals.
 * Field sets are aligned by design, so MapStruct auto-maps every property.
 *
 * @author pnoker
 * @since 2026.5.5
 */
@Mapper(componentModel = "spring", uses = {MapStructUtil.class})
public interface FacadeDeviceBuilder {

    DeviceQuery toManagerQuery(FacadeDeviceQuery facadeQuery);

    FacadeDeviceBO toFacadeBO(DeviceBO managerBO);
}
