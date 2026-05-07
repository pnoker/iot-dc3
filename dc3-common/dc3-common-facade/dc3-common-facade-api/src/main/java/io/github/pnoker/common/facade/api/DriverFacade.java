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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadeDriverBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadeDriverQuery;

/**
 * Protocol-neutral driver facade.
 * <p>
 * Mirrors the three RPCs on {@code api.center.manager.DriverApi}. Implementation
 * selection follows the same {@code dc3.facade.mode} switch used by {@link DeviceFacade}.
 *
 * @author pnoker
 * @since 2026.5.5
 */
public interface DriverFacade {

    /**
     * @return the driver, or {@code null} when it does not exist.
     */
    FacadeDriverBO selectById(Long id);

    /**
     * @return a page of drivers (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadeDriverBO> selectByPage(FacadeDriverQuery query);

    /**
     * Resolve the driver that owns a given device.
     *
     * @return the driver, or {@code null} when the device has no bound driver.
     */
    FacadeDriverBO selectByDeviceId(Long deviceId);

}
