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
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;

/**
 * 元数据 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class MetadataConstant {

    private MetadataConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 模板元数据 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Profile {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.PROFILE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.PROFILE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.PROFILE;

        private Profile() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 模板元数据 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Point {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT;

        private Point() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 设备元数据 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class Device {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DEVICE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DEVICE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DEVICE;

        private Device() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 驱动信息元数据 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class DriverConfig {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DRIVER_ATTRIBUTE_CONFIG;

        private DriverConfig() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

    /**
     * 位号信息元数据 相关常量
     *
     * @author pnoker
     * @version 2025.6.0
     * @since 2022.1.0
     */
    public static class PointConfig {

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT_ATTRIBUTE_CONFIG;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT_ATTRIBUTE_CONFIG;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT_ATTRIBUTE_CONFIG;

        private PointConfig() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }
    }

}
