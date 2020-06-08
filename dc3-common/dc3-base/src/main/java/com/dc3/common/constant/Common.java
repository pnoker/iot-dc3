/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
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
     * 对称加密算法
     */
    String KEY_ALGORITHM_AES = "AES";

    /**
     * 非对称加密算法
     */
    String KEY_ALGORITHM_RSA = "RSA";

    /**
     * 时间格式化
     */
    String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时区
     */
    String TIMEZONE = "GMT+8";

    /**
     * 默认密码
     */
    String DEFAULT_PASSWORD = "dc3dc3dc3";

    /**
     * 消息常量
     */
    interface Rabbit {
        String TOPIC_EXCHANGE = "dc3.exchange";
        String POINT_VALUE_QUEUE = "point.value.queue";
    }

    /**
     * 数据类型
     */
    interface ValueType {
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
         * dc3-auth 服务
         */
        String DC3_AUTH_SERVICE_NAME = "DC3-AUTH";
        String DC3_AUTH_USER_URL_PREFIX = "/auth/user";
        String DC3_AUTH_TOKEN_URL_PREFIX = "/auth/token";
        String DC3_AUTH_BLACK_IP_URL_PREFIX = "/auth/blackIp";

        /**
         * dc3-manager 服务
         */
        String DC3_MANAGER_SERVICE_NAME = "DC3-MANAGER";
        String DC3_MANAGER_DRIVER_URL_PREFIX = "/manager/driver";
        String DC3_MANAGER_DRIVER_ATTRIBUTE_URL_PREFIX = "/manager/driverAttribute";
        String DC3_MANAGER_POINT_ATTRIBUTE_URL_PREFIX = "/manager/pointAttribute";
        String DC3_MANAGER_PROFILE_URL_PREFIX = "/manager/profile";
        String DC3_MANAGER_POINT_URL_PREFIX = "/manager/point";
        String DC3_MANAGER_GROUP_URL_PREFIX = "/manager/group";
        String DC3_MANAGER_DEVICE_URL_PREFIX = "/manager/device";
        String DC3_MANAGER_POINT_INFO_URL_PREFIX = "/manager/pointInfo";
        String DC3_MANAGER_DRIVER_INFO_URL_PREFIX = "/manager/driverInfo";
        String DC3_MANAGER_LABEL_URL_PREFIX = "/manager/label";
        String DC3_MANAGER_DICTIONARY_URL_PREFIX = "/manager/dictionary";

        /**
         * dc3-rtmp 服务
         */
        String DC3_RTMP_SERVICE_NAME = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";

        /**
         * dc3-data 服务
         */
        String DC3_DATA_SERVICE_NAME = "DC3-DATA";
        String DC3_DATA_URL_PREFIX = "/data";
        String DC3_DRIVER_URL_PREFIX = "/driver";
    }

    /**
     * 缓存Key
     */
    interface Cache {
        /**
         * token 在 redis 中的失效时间
         */
        int TOKEN_CACHE_TIMEOUT = 5;

        /**
         * 分隔符
         */
        String SEPARATOR = "::";

        String ID = "_id";
        String IP = "_ip";
        String NAME = "_name";
        String CODE = "_code";
        String DIC = "_dic";
        String LIST = "_list";
        String SALT = "_salt";
        String TOKEN = "_token";
        String LIMIT = "_limit";
        String SERVICE_NAME = "_service_name";
        String HOST_PORT = "_host_port";
        String DRIVER_INFO_ID = "_driver_info_id";
        String POINT_INFO_ID = "_point_info_id";
        String GROUP_NAME = "_group_name";

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
    }

}
