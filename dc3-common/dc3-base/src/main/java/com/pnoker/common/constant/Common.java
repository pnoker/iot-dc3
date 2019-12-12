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
 * <p>dc3平台常量
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface Common {

    /**
     * 标志
     */
    String FROM = "from";

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
    String DATEFORMAT = "yyyy-MM-dd HH:mm:ss";
    /**
     * 时区
     */
    String TIMEZONE = "GMT+8";

    /**
     * 服务名称 & 服务基地址
     */
    interface Service {
        String DC3_DBS = "DC3-DBS";
        String DC3_DBS_RTMP_URL_PREFIX = "/api/v3/center/dbs/rtmp";
        String DC3_DBS_USER_URL_PREFIX = "/api/v3/center/dbs/user";

        String DC3_RTMP = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/api/v3/transfer/rtmp";

        String DC3_AUTH = "DC3-AUTH";
        String DC3_AUTH_URL_PREFIX = "/api/v3/center/auth";
    }

    /**
     * 数据字段
     */
    interface Cloumn {
        String ID = "id";
        String NAME = "name";
        String CREATE_TIME = "create_time";
        String UPDATE_TIME = "update_time";

        /**
         * User表
         */
        interface User {
            String USERNAME = "username";
            String PHONE = "phone";
            String EMAIL = "email";
        }

        /**
         * Rtmp表
         */
        interface Rtmp {
            String AUTO_START = "auto_start";
        }

    }

}
