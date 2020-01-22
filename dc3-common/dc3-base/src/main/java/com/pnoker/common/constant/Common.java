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
     * 服务名称 & 服务基地址
     */
    interface Service {
        String DC3_AUTH = "DC3-AUTH";
        String DC3_USER_URL_PREFIX = "/auth/user";
        String DC3_TOKEN_URL_PREFIX = "/auth/token";

        String DC3_MANAGER = "DC3_MANAGER";
        String DC3_MANAGER_DEVICE_URL_PREFIX = "/manager/device";
        String DC3_MANAGER_GROUP_URL_PREFIX = "/manager/group";
        String DC3_MANAGER_POINT_URL_PREFIX = "/manager/point";

        String DC3_RTMP = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";
    }

    /**
     * 缓存Key
     */
    interface Cache {
        String USER_ID = "auth_user_id";
        String USER_NAME = "auth_user_name";
        String USER_LIST = "auth_user_list";

        String GROUP_ID = "manager_group_id";
        String GROUP_NAME = "manager_group_name";
        String GROUP_LIST = "manager_group_list";

        String PROFILE_ID = "manager_profile_id";
        String PROFILE_NAME = "manager_profile_name";
        String PROFILE_LIST = "manager_profile_list";

        String POINT_ID = "manager_point_id";
        String POINT_NAME = "manager_point_name";
        String POINT_LIST = "manager_point_list";

        String DEVICE_ID = "manager_device_id";
        String DEVICE_CODE = "manager_device_code";
        String DEVICE_GROUP_NAME = "manager_device_group_name";
        String DEVICE_LIST = "manager_device_list";

        String RTMP_ID = "transfer_rtmp_id";
        String RTMP_LIST = "transfer_rtmp_list";
    }

}
