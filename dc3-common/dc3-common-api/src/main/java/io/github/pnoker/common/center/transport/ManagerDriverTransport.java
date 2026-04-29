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
import io.github.pnoker.api.center.manager.GrpcPageDriverQuery;
import io.github.pnoker.api.center.manager.GrpcRDriverDTO;
import io.github.pnoker.api.center.manager.GrpcRPageDriverDTO;

/**
 * Manager driver transport abstraction.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface ManagerDriverTransport {

    /**
     * Query drivers by page.
     *
     * @param request page query
     * @return page result
     */
    GrpcRPageDriverDTO selectByPage(GrpcPageDriverQuery request);

    /**
     * Query a driver by device id.
     *
     * @param request device query
     * @return driver result
     */
    GrpcRDriverDTO selectByDeviceId(GrpcDeviceQuery request);

    /**
     * Query a driver by driver id.
     *
     * @param request driver query
     * @return driver result
     */
    GrpcRDriverDTO selectByDriverId(GrpcDriverQuery request);
}

