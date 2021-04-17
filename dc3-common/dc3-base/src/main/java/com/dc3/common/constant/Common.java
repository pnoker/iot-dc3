/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.constant;

/**
 * dc3平台常量
 *
 * @author pnoker
 */
public interface Common {

    /**
     * 默认密钥
     */
    String KEY = "pnoker/dc3";

    /**
     * 默认密码
     */
    String DEFAULT_PASSWORD = "dc3dc3dc3";

    /**
     * 对称加密算法
     */
    String KEY_ALGORITHM_AES = "AES";

    /**
     * 非对称加密算法
     */
    String KEY_ALGORITHM_RSA = "RSA";

    /**
     * 时区
     */
    String TIMEZONE = "GMT+8";

    /**
     * 时间格式化
     */
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 用户主目录
     */
    String USER_HOME_PATH = System.getProperty("user.home") + "/.dc3/";

    /**
     * 默认上传文件的缓存位置
     */
    String TEMP_FILE_PATH = System.getProperty("java.io.tmpdir") + "/dc3/";

    /**
     * 应答语
     */
    interface Response {
        String OK = "ok";
        String ERROR = "error";
    }

    /**
     * 消息常量
     */
    interface Rabbit {
        // Event
        String TOPIC_EXCHANGE_EVENT = "dc3.exchange.event";
        String ROUTING_DRIVER_EVENT_PREFIX = "dc3.routing.event.driver.";
        String QUEUE_DRIVER_EVENT = "dc3.queue.event.driver";
        String ROUTING_DEVICE_EVENT_PREFIX = "dc3.routing.event.device.";
        String QUEUE_DEVICE_EVENT = "dc3.queue.event.device";

        // Metadata
        String TOPIC_EXCHANGE_METADATA = "dc3.exchange.metadata";
        String ROUTING_DRIVER_METADATA_PREFIX = "dc3.routing.metadata.driver.";
        String QUEUE_DRIVER_METADATA_PREFIX = "dc3.queue.metadata.driver.";

        // Value
        String TOPIC_EXCHANGE_VALUE = "dc3.exchange.value";
        String ROUTING_POINT_VALUE_PREFIX = "dc3.routing.value.point.";
        String QUEUE_POINT_VALUE = "dc3.queue.value.point";
    }

    /**
     * 驱动常量
     */
    interface Driver {
        int MAX_REQUEST_SIZE = 100;

        /**
         * 设备状态
         */
        interface Status {
            String REGISTERING = "REGISTERING";
            String UNREGISTERED = "UNREGISTERED";

            String ONLINE = "ONLINE";
            String OFFLINE = "OFFLINE";
            String FAULT = "FAULT";
        }

        /**
         * 驱动事件
         */
        interface Event {
            /**
             * 驱动注册握手事件，该事件用于校验当前 dc3-manager 是否可用
             */
            String DRIVER_HANDSHAKE = "driver_handshake";
            String DRIVER_HANDSHAKE_BACK = "driver_handshake_back";

            /**
             * 驱动注册事件，该事件用于向 dc3-manager 注册驱动配置信息
             */
            String DRIVER_REGISTER = "driver_register";
            String DRIVER_REGISTER_BACK = "driver_register_back";

            /**
             * 同步驱动元数据时间，该事件用于向 dc3-manager 发送驱动元数据同步请求
             */
            String DRIVER_METADATA_SYNC = "driver_metadata_sync";
            String DRIVER_METADATA_SYNC_BACK = "driver_metadata_sync_back";

            /**
             * 驱动心跳事件，该事件用于向 dc3-manager 发送驱动的当前状态
             */
            String HEARTBEAT = "heartbeat";

            String ERROR = "error";
        }

        interface Type {
            String DRIVER = "driver";
            String PROFILE = "profile";
            String DEVICE = "device";
            String POINT = "point";
            String DRIVER_INFO = "driver_info";
            String POINT_INFO = "point_info";
        }

        interface Profile {
            String ADD = "add_profile";
            String DELETE = "delete_profile";
            String UPDATE = "update_profile";
        }

        interface Device {
            String ADD = "add_device";
            String DELETE = "delete_device";
            String UPDATE = "update_device";
        }

        interface Point {
            String ADD = "add_point";
            String DELETE = "delete_point";
            String UPDATE = "update_point";
        }

        interface DriverInfo {
            String ADD = "add_driver_info";
            String DELETE = "delete_driver_info";
            String UPDATE = "update_driver_info";
        }

        interface PointInfo {
            String ADD = "add_point_info";
            String DELETE = "delete_point_info";
            String UPDATE = "update_point_info";
        }
    }

    /**
     * 设备常量
     */
    interface Device {

        /**
         * 设备状态
         */
        interface Status {
            String ONLINE = "ONLINE";
            String OFFLINE = "OFFLINE";
            String MAINTAIN = "MAINTAIN";
            String FAULT = "FAULT";
        }

        /**
         * 设备事件
         */
        interface Event {
            /**
             * 设备心跳事件
             */
            String HEARTBEAT = "heartbeat";

            /**
             * 超出上限事件
             */
            String OVER_UPPER_LIMIT = "over_upper_limit";

            /**
             * 超出下限事件
             */
            String OVER_LOWER_LIMIT = "over_lower_limit";

            /**
             * 用于记录错误事件类型
             */
            String ERROR = "error";
        }
    }

    /**
     * 数据类型
     */
    interface ValueType {
        String HEX = "hex";
        String BYTE = "byte";
        String SHORT = "short";
        String INT = "int";
        String LONG = "long";
        String FLOAT = "float";
        String DOUBLE = "double";
        String BOOLEAN = "boolean";
        String STRING = "string";
    }

    /**
     * 服务名称 & 服务基地址
     */
    interface Service {
        /**
         * dc3-gateway 服务
         */
        String DC3_GATEWAY_AUTH_USER = "X-Auth-User";
        String DC3_GATEWAY_AUTH_SALT = "X-Auth-Salt";
        String DC3_GATEWAY_AUTH_TOKEN = "X-Auth-Token";

        /**
         * dc3-auth 服务
         */
        String DC3_AUTH_SERVICE_NAME = "DC3-AUTH";
        String DC3_AUTH_USER_URL_PREFIX = "/auth/user";
        String DC3_AUTH_TOKEN_URL_PREFIX = "/auth/token";
        String DC3_AUTH_BLACK_IP_URL_PREFIX = "/auth/black_ip";

        /**
         * dc3-manager 服务
         */
        String DC3_MANAGER_SERVICE_NAME = "DC3-MANAGER";
        String DC3_MANAGER_DRIVER_URL_PREFIX = "/manager/driver";
        String DC3_MANAGER_BATCH_URL_PREFIX = "/manager/batch";
        String DC3_MANAGER_DRIVER_ATTRIBUTE_URL_PREFIX = "/manager/driver_attribute";
        String DC3_MANAGER_POINT_ATTRIBUTE_URL_PREFIX = "/manager/point_attribute";
        String DC3_MANAGER_PROFILE_URL_PREFIX = "/manager/profile";
        String DC3_MANAGER_POINT_URL_PREFIX = "/manager/point";
        String DC3_MANAGER_GROUP_URL_PREFIX = "/manager/group";
        String DC3_MANAGER_DEVICE_URL_PREFIX = "/manager/device";
        String DC3_MANAGER_POINT_INFO_URL_PREFIX = "/manager/point_info";
        String DC3_MANAGER_DRIVER_INFO_URL_PREFIX = "/manager/driver_info";
        String DC3_MANAGER_LABEL_URL_PREFIX = "/manager/label";
        String DC3_MANAGER_DICTIONARY_URL_PREFIX = "/manager/dictionary";
        String DC3_MANAGER_STATUS_URL_PREFIX = "/manager/status";
        String DC3_MANAGER_EVENT_URL_PREFIX = "/manager/event";

        /**
         * dc3-rtmp 服务
         */
        String DC3_RTMP_SERVICE_NAME = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";

        /**
         * dc3-data 服务
         */
        String DC3_DATA_SERVICE_NAME = "DC3-DATA";
        String DC3_DATA_POINT_VALUE_URL_PREFIX = "/data/point_value";

        /**
         * dc3-driver-sdk 服务
         */
        String DC3_DRIVER_URL_PREFIX = "/driver";

    }

    /**
     * 缓存Key
     */
    interface Cache {
        /**
         * salt 在 redis 中的失效时间，分钟
         */
        int SALT_CACHE_TIMEOUT = 5;

        /**
         * user 登陆限制失效时间，分钟
         */
        int USER_LIMIT_TIMEOUT = 5;

        /**
         * token 在 redis 中的失效时间，小时
         */
        int TOKEN_CACHE_TIMEOUT = 12;

        /**
         * 点
         */
        String DOT = ".";

        /**
         * 星号
         */
        String ASTERISK = "*";

        /**
         * 分隔符
         */
        String SEPARATOR = "::";

        String ID = "_id";
        String DRIVER_ID = "_driver_id";
        String PROFILE_ID = "_profile_id";
        String GROUP_ID = "_group_id";
        String DEVICE_ID = "_device_id";
        String POINT_ID = "_point_id";
        String ATTRIBUTE_ID = "_attribute_id";

        String IP = "_ip";
        String NAME = "_name";
        String STATUS = "_status";
        String VALUE = "_value";
        String VALUES = "_values";
        String DIC = "_dic";
        String LIST = "_list";
        String SALT = "_salt";
        String TOKEN = "_token";
        String LIMIT = "_limit";
        String HOST_PORT = "_host_port";
        String SERVICE_NAME = "_service_name";

        String DRIVER = "driver";
        String DRIVER_ATTRIBUTE = "driver_attribute";
        String POINT_ATTRIBUTE = "point_attribute";
        String PROFILE = "profile";
        String DRIVER_INFO = "driver_info";
        String POINT = "point";
        String GROUP = "group";
        String DEVICE = "device";
        String POINT_INFO = "point_info";
        String USER = "user";
        String RTMP = "rtmp";
        String LABEL = "label";
        String LABEL_BIND = "label_bind";
        String BLACK_IP = "black_ip";

        String REAL_TIME_VALUE_KEY_PREFIX = Common.Cache.POINT + Common.Cache.VALUE + Common.Cache.SEPARATOR;
        String DRIVER_STATUS_KEY_PREFIX = Common.Cache.DRIVER + Common.Cache.STATUS + Common.Cache.SEPARATOR;
        String DEVICE_STATUS_KEY_PREFIX = Common.Cache.DEVICE + Common.Cache.STATUS + Common.Cache.SEPARATOR;
    }

}
