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

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 事件 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class EventConstant {

    private EventConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 驱动事件 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Driver {

        /**
         * 驱动状态事件, 该事件用于向 dc3-center-manager 发送驱动的当前状态
         */
        public static final String STATUS = "driver_status";
        /**
         * 驱动注册事件, 该事件用于向 dc3-center-manager 注册驱动配置信息
         */
        public static final String REGISTER = "driver_register";
        public static final String REGISTER_BACK = "driver_register_back";

        private Driver() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

    }

    /**
     * 设备事件 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Device {

        /**
         * 设备状态事件
         */
        public static final String STATUS = "device_status";
        /**
         * 用于记录错误事件类型
         */
        public static final String ERROR = "device_error";

        private Device() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

    }

}
