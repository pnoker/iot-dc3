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

package io.github.pnoker.common.center.transport;

import io.github.pnoker.api.center.manager.GrpcDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcDriverQuery;
import io.github.pnoker.api.center.manager.GrpcPageDeviceQuery;
import io.github.pnoker.api.center.manager.GrpcProfileQuery;
import io.github.pnoker.api.center.manager.GrpcRDeviceDTO;
import io.github.pnoker.api.center.manager.GrpcRDeviceListDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDeviceDTO;

/**
 * Manager device transport abstraction.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface ManagerDeviceTransport {

    /**
     * Query devices by page.
     *
     * @param request page query
     * @return page result
     */
    GrpcRPageDeviceDTO selectByPage(GrpcPageDeviceQuery request);

    /**
     * Query devices by driver id.
     *
     * @param request driver query
     * @return device list result
     */
    GrpcRDeviceListDTO selectByDriverId(GrpcDriverQuery request);

    /**
     * Query devices by profile id.
     *
     * @param request profile query
     * @return device list result
     */
    GrpcRDeviceListDTO selectByProfileId(GrpcProfileQuery request);

    /**
     * Query a device by device id.
     *
     * @param request device query
     * @return device result
     */
    GrpcRDeviceDTO selectByDeviceId(GrpcDeviceQuery request);
}

