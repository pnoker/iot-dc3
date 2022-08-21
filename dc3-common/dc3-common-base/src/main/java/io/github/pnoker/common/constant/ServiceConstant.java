/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant;

/**
 * 服务相关
 *
 * @author pnoker
 */
public interface ServiceConstant {

    /**
     * 请求 Header 相关
     */
    interface Header {
        String X_AUTH_TENANT_ID = "X-Auth-Tenant-Id";
        String X_AUTH_TENANT = "X-Auth-Tenant";
        String X_AUTH_USER_ID = "X-Auth-User-Id";
        String X_AUTH_USER = "X-Auth-User";
        String X_AUTH_TOKEN = "X-Auth-Token";
    }

    /**
     * 权限服务相关
     */
    interface Auth {
        String SERVICE_NAME = "DC3-CENTER-AUTH";

        String USER_URL_PREFIX = "/auth/user";
        String TENANT_URL_PREFIX = "/auth/tenant";
        String TOKEN_URL_PREFIX = "/auth/token";
        String BLACK_IP_URL_PREFIX = "/auth/black_ip";
        String DICTIONARY_URL_PREFIX = "/auth/dictionary";
    }

    /**
     * 管理服务相关
     */
    interface Manager {
        String SERVICE_NAME = "DC3-CENTER-MANAGER";

        String DRIVER_URL_PREFIX = "/manager/driver";
        String BATCH_URL_PREFIX = "/manager/batch";
        String DRIVER_ATTRIBUTE_URL_PREFIX = "/manager/driver_attribute";
        String POINT_ATTRIBUTE_URL_PREFIX = "/manager/point_attribute";
        String PROFILE_URL_PREFIX = "/manager/profile";
        String POINT_URL_PREFIX = "/manager/point";
        String GROUP_URL_PREFIX = "/manager/group";
        String DEVICE_URL_PREFIX = "/manager/device";
        String AUTO_URL_PREFIX = "/manager/auto";
        String POINT_INFO_URL_PREFIX = "/manager/point_info";
        String DRIVER_INFO_URL_PREFIX = "/manager/driver_info";
        String LABEL_URL_PREFIX = "/manager/label";
        String DICTIONARY_URL_PREFIX = "/manager/dictionary";
        String STATUS_URL_PREFIX = "/manager/status";
        String EVENT_URL_PREFIX = "/manager/event";
    }

    /**
     * 数据服务相关
     */
    interface Data {
        String SERVICE_NAME = "DC3-CENTER-DATA";

        String VALUE_URL_PREFIX = "/data/point_value";
    }

    /**
     * 视频服务相关
     */
    interface Rtmp {
        String SERVICE_NAME = "DC3-TRANSFER-RTMP";

        String URL_PREFIX = "/transfer/rtmp";
    }

    /**
     * 驱动服务相关
     */
    interface Driver {
        String COMMAND_URL_PREFIX = "/driver/command";
    }


}
