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
 * Driver registration service, invoked over gRPC when a driver process announces itself.
 * <p>
 * The gRPC path carries no HTTP security context, so the caller's tenant is taken from
 * the registration payload and bound explicitly to the tenant context for each method.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface DriverRegisterService {

    /**
     * Register or update a driver, keyed by service name within the payload's tenant.
     * Updates the existing record when the service name is already registered, otherwise
     * creates it, then returns the latest driver.
     *
     * @param entityGrpc {@link GrpcDriverRegisterDTO} carrying the driver and its tenant code
     * @return the registered driver
     */
    DriverBO registerDriver(GrpcDriverRegisterDTO entityGrpc);

    /**
     * Synchronize a driver's driver attributes via a three-way diff (insert new, update
     * changed, remove obsolete), keyed by attribute code within the driver's tenant.
     *
     * @param entityGrpc {@link GrpcDriverRegisterDTO} carrying the declared driver attributes
     * @param entityBO   the owning driver
     * @return the driver attributes currently registered for the driver
     */
    List<DriverAttributeBO> registerDriverAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Synchronize a driver's point attributes via a three-way diff (insert new, update
     * changed, remove obsolete), keyed by attribute code within the driver's tenant.
     *
     * @param entityGrpc {@link GrpcDriverRegisterDTO} carrying the declared point attributes
     * @param entityBO   the owning driver
     * @return the point attributes currently registered for the driver
     */
    List<PointAttributeBO> registerPointAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Synchronize a driver's command attributes via a three-way diff (insert new, update
     * changed, remove obsolete), keyed by attribute code within the driver's tenant.
     *
     * @param entityGrpc {@link GrpcDriverRegisterDTO} carrying the declared command attributes
     * @param entityBO   the owning driver
     * @return the command attributes currently registered for the driver
     */
    List<CommandAttributeBO> registerCommandAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

    /**
     * Synchronize a driver's event attributes via a three-way diff (insert new, update
     * changed, remove obsolete), keyed by attribute code within the driver's tenant.
     *
     * @param entityGrpc {@link GrpcDriverRegisterDTO} carrying the declared event attributes
     * @param entityBO   the owning driver
     * @return the event attributes currently registered for the driver
     */
    List<EventAttributeBO> registerEventAttribute(GrpcDriverRegisterDTO entityGrpc, DriverBO entityBO);

}
