/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant;

import io.github.pnoker.common.constant.common.ExceptionConstant;

/**
 * 事件 相关常量
 *
 * @author pnoker
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
     * @since 2022.1.0
     */
    public static class Driver {

        private Driver() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        /**
         * 驱动状态事件，该事件用于向 dc3-center-manager 发送驱动的当前状态
         */
        public static final String STATUS = "driver_status";
        /**
         * 驱动注册握手事件，该事件用于校验当前 dc3-center-manager 是否可用
         */
        public static final String HANDSHAKE = "driver_handshake";
        public static final String HANDSHAKE_BACK = "driver_handshake_back";
        /**
         * 驱动注册事件，该事件用于向 dc3-center-manager 注册驱动配置信息
         */
        public static final String REGISTER = "driver_register";
        public static final String REGISTER_BACK = "driver_register_back";
        /**
         * 同步驱动元数据时间，该事件用于向 dc3-center-manager 发送驱动元数据同步请求
         */
        public static final String METADATA_SYNC = "driver_metadata_sync";
        public static final String METADATA_SYNC_BACK = "driver_metadata_sync_back";

        /**
         * 用于记录错误事件类型
         */
        public static final String ERROR = "driver_error";

    }

    /**
     * 设备事件 相关常量
     *
     * @author pnoker
     * @since 2022.1.0
     */
    public static class Device {

        private Device() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        /**
         * 设备状态事件
         */
        public static final String STATUS = "device_status";

        /**
         * 用于记录错误事件类型
         */
        public static final String ERROR = "device_error";

    }

}
