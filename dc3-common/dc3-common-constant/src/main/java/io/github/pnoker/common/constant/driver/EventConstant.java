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

package io.github.pnoker.common.constant.driver;

import io.github.pnoker.common.constant.common.BaseConstant;


/**
 * Event related constants
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class EventConstant {

    private EventConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

    /**
     * Driver event related constants
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class Driver {

        /**
         * Driver status event, used to send the current status of the driver to
         * dc3-center-manager
         */
        public static final String STATUS = "driver_status";

        /**
         * Driver registration event, used to register driver configuration information to
         * dc3-center-manager
         */
        public static final String REGISTER = "driver_register";

        public static final String REGISTER_BACK = "driver_register_back";

        private Driver() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

    /**
     * Device event related constants
     *
     * @author pnoker
     * @version 2025.9.0
     * @since 2022.1.0
     */
    public static class Device {

        /**
         * Device status event
         */
        public static final String STATUS = "device_status";

        /**
         * Used to record error event type
         */
        public static final String ERROR = "device_error";

        private Device() {
            throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
        }

    }

}
