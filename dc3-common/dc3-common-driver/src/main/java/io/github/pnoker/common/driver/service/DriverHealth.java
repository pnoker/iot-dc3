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

package io.github.pnoker.common.driver.service;

import io.github.pnoker.common.driver.entity.bean.DriverHealthState;

/**
 * Driver-level health hook invoked by the SDK's fixed driver health schedule.
 *
 * <p>Process liveness is still protected by Data Center timeout scanning. This
 * hook lets a running driver report protocol/runtime health such as
 * {@code FAULT}, {@code MAINTAIN}, or an explicit {@code OFFLINE} state.
 *
 * @author pnoker
 * @version 2026.5.22
 * @since 2026.5.22
 */
public interface DriverHealth {

    /**
     * Determine driver health from the protocol driver's point of view.
     *
     * @return driver health state
     */
    default DriverHealthState health() {
        return DriverHealthState.online();
    }

}
