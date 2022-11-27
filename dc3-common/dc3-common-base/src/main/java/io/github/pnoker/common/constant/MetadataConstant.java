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
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.common.SuffixConstant;

/**
 * 元数据 相关常量
 *
 * @author pnoker
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
     * @since 2022.1.0
     */
    public static class Profile {

        private Profile() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.PROFILE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.PROFILE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.PROFILE;
    }

    /**
     * 模板元数据 相关常量
     *
     * @author pnoker
     * @since 2022.1.0
     */
    public static class Point {

        private Point() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT;
    }

    /**
     * 设备元数据 相关常量
     *
     * @author pnoker
     * @since 2022.1.0
     */
    public static class Device {

        private Device() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DEVICE;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DEVICE;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DEVICE;
    }

    /**
     * 驱动信息元数据 相关常量
     *
     * @author pnoker
     * @since 2022.1.0
     */
    public static class DriverInfo {

        private DriverInfo() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.DRIVER_INFO;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.DRIVER_INFO;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.DRIVER_INFO;
    }

    /**
     * 位号信息元数据 相关常量
     *
     * @author pnoker
     * @since 2022.1.0
     */
    public static class PointInfo {

        private PointInfo() {
            throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
        }

        public static final String ADD = PrefixConstant.ADD + SuffixConstant.POINT_INFO;
        public static final String DELETE = PrefixConstant.DELETE + SuffixConstant.POINT_INFO;
        public static final String UPDATE = PrefixConstant.UPDATE + SuffixConstant.POINT_INFO;
    }

}
