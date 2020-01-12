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

        String RTMP_ID = "transfer_rtmp_id";
        String RTMP_LIST = "transfer_rtmp_list";
    }

}
