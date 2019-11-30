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
public interface CommonConstants {

    /**
     * 标志
     */
    String FROM = "from";

    /**
     * 默认登录URL
     */
    String OAUTH_TOKEN_URL = "/oauth/token";

    /**
     * 数据字段
     */
    interface Cloumn {
        String NAME = "name";

        /**
         * 用户表
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

        /**
         * 通用描述信息
         */
        interface Description {
            String ID = "id";
        }
    }

}
