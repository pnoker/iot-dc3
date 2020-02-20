/*
 * Copyright 2019 Pnoker. All Rights Reserved.
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

package com.pnoker.common.constant;

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
    String DEFAULT_PASSWORD = "dc3dc3";

    /**
     * 消息通道
     */
    interface Topic {
        String DRIVER_TOPIC = "driver.topic";
    }

    /**
     * 驱动 SDK Job Bean 前缀 和 三个固定调度任务
     */
    interface Sdk {
        String JOB_PREFIX = "com.pnoker.common.sdk.quartz.job.";
        String CUSTOMIZER_JOB = "CustomizerJob";
        String READ_JOB = "ReadJob";
        String WRITE_JOB = "WriteJob";
    }

    /**
     * 属性常量
     */
    interface Property {
        short READ_ONLY = 0;
        short WRITE_ONLY = 1;
        short READ_WRITE = 2;
    }

    /**
     * 服务名称 & 服务基地址
     */
    interface Service {
        String DC3_AUTH = "DC3-AUTH";
        String DC3_USER_URL_PREFIX = "/auth/user";
        String DC3_TOKEN_URL_PREFIX = "/auth/token";

        String DC3_MANAGER = "DC3-MANAGER";
        String DC3_MANAGER_DRIVER_URL_PREFIX = "/manager/driver";
        String DC3_MANAGER_DRIVER_ATTRIBUTE_URL_PREFIX = "/manager/driverAttribute";
        String DC3_MANAGER_POINT_ATTRIBUTE_URL_PREFIX = "/manager/pointAttribute";
        String DC3_MANAGER_PROFILE_URL_PREFIX = "/manager/profile";
        String DC3_MANAGER_POINT_URL_PREFIX = "/manager/point";
        String DC3_MANAGER_GROUP_URL_PREFIX = "/manager/group";
        String DC3_MANAGER_DEVICE_URL_PREFIX = "/manager/device";
        String DC3_MANAGER_POINT_INFO_URL_PREFIX = "/manager/pointInfo";
        String DC3_MANAGER_DRIVER_INFO_URL_PREFIX = "/manager/driverInfo";
        String DC3_MANAGER_SCHEDULE_URL_PREFIX = "/manager/schedule";
        String DC3_MANAGER_LABEL_URL_PREFIX = "/manager/label";

        String DC3_RTMP = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";

        String DC3_DATA_URL_PREFIX = "/data";
        String DC3_DRIVER_URL_PREFIX = "/driver";
    }

    /**
     * 缓存Key
     */
    interface Cache {
        String ID = "_id";
        String DIC = "_dic";
        String NAME = "_name";
        String CODE = "_code";
        String LIST = "_list";
        String HOST_PORT = "_host_port";
        String GROUP_NAME = "_group_name";
        String SERVICE_NAME = "_service_name";
        String PROFILE_INFO_ID = "_profile_info_id";
        String CONNECT_INFO_ID = "_connect_info_id";

        String USER = "user";
        String RTMP = "rtmp";
        String GROUP = "group";
        String SCHEDULE = "schedule";
        String POINT = "point";
        String LABEL = "label";
        String DEVICE = "device";
        String DRIVER = "driver";
        String PROFILE = "profile";
        String DRIVER_ATTRIBUTE = "driver_attribute";
        String DRIVER_INFO = "driver_info";
        String POINT_ATTRIBUTE = "point_attribute";
        String POINT_INFO = "point_info";
        String LABEL_BIND = "label_bind";
    }

}
