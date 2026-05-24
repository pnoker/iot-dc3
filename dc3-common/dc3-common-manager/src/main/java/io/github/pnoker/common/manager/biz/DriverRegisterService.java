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

package io.github.pnoker.common.manager.biz;

import io.github.pnoker.api.common.driver.GrpcDriverRegisterDTO;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.EventAttributeBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;

import java.util.List;

/**
 * Driver registration interfaces
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverRegisterService {

    /**
     * Register driver
     *
     * @param entityGrpc GrpcDriverRegisterDTO
     */
    DriverBO registerDriver(GrpcDriverRegisterDTO entityGrpc);

    /**
     * Register driver attributes
     *
     * @param entityGrpc GrpcDriverRegisterDTO
     * @param entityBO   DriverBO
     */
    List<DriverAttributeBO> registerDriverAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Register point attributes
     *
     * @param entityGrpc GrpcDriverRegisterDTO
     * @param entityBO   DriverBO
     */
    List<PointAttributeBO> registerPointAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Register command attributes
     *
     * @param entityGrpc GrpcDriverRegisterDTO
     * @param entityBO   DriverBO
     */
    List<CommandAttributeBO> registerCommandAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Register event attributes
     *
     * @param entityGrpc GrpcDriverRegisterDTO
     * @param entityBO   DriverBO
     */
    List<EventAttributeBO> registerEventAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

}
