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

        String DC3_DEVICE = "DC3-DEVICE";
        String DC3_DEVICE_DRIVER_URL_PREFIX = "/device/driver";
        String DC3_DEVICE_CONNECT_INFO_URL_PREFIX = "/device/connectInfo";
        String DC3_DEVICE_PROFILE_INFO_URL_PREFIX = "/device/profileInfo";
        String DC3_DEVICE_PROFILE_URL_PREFIX = "/device/profile";
        String DC3_DEVICE_POINT_URL_PREFIX = "/device/point";
        String DC3_DEVICE_GROUP_URL_PREFIX = "/device/group";
        String DC3_DEVICE_DEVICE_URL_PREFIX = "/device/device";
        String DC3_DEVICE_POINT_INFO_URL_PREFIX = "/device/pointInfo";
        String DC3_DEVICE_LABEL_URL_PREFIX = "/device/label";
        String DC3_DEVICE_DIC_URL_PREFIX = "/device/dic";

        String DC3_RTMP = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";
    }

    /**
     * 缓存Key
     */
    interface Cache {
        String ID = "_id";
        String NAME = "_name";
        String SERVICE_NAME = "_service_name";
        //String GROUP_NAME = "_group_name";
        String DIC = "_dic";
        String LIST = "_list";

        String USER = "auth_user";


        String GROUP_ID = "device_group_id";
        String GROUP_NAME = "device_group_name";
        String GROUP_DIC = "device_group_dic";
        String GROUP_LIST = "device_group_list";

        String DRIVER_ID = "device_driver_id";
        String DRIVER_SERVICE_NAME = "device_driver_service_name";
        String DRIVER_DIC = "device_driver_dir";
        String DRIVER_LIST = "device_driver_list";

        String PROFILE_ID = "device_profile_id";
        String PROFILE_NAME = "device_profile_name";
        String PROFILE_DIC = "device_profile_dic";
        String PROFILE_LIST = "device_profile_list";

        String POINT_ID = "device_point_id";
        String POINT_NAME = "device_point_name";
        String POINT_DIC = "device_point_dic";
        String POINT_LIST = "device_point_list";

        String DEVICE_ID = "device_device_id";
        String DEVICE_CODE = "device_device_code";
        String DEVICE_DIC = "device_device_dic";
        String DEVICE_GROUP_NAME = "device_device_group_name";
        String DEVICE_LIST = "device_device_list";

        String RTMP_ID = "transfer_rtmp_id";
        String RTMP_DIC = "transfer_rtmp_dic";
        String RTMP_LIST = "transfer_rtmp_list";

        String LABEL_ID = "device_label_id";
        String LABEL_NAME = "device_label_name";
        String LABEL_DIC = "device_label_dic";
        String LABEL_LIST = "device_label_list";

        String LABEL_BIND_ID = "device_label_bind_id";
        String LABEL_BIND_DIC = "device_label_bind_dic";
        String LABEL_BIND_LIST = "device_label_bind_list";

        String CONNECT_INFO_ID = "device_connect_info_id";
        String CONNECT_INFO_NAME = "device_connect_info_name";
        String CONNECT_INFO_DIC = "device_connect_info_dic";
        String CONNECT_INFO_LIST = "device_connect_info_list";

        String PROFILE_INFO_ID = "device_profile_info_id";
        String PROFILE_INFO_NAME = "device_profile_info_name";
        String PROFILE_INFO_DIC = "device_profile_info_dic";
        String PROFILE_INFO_LIST = "device_profile_info_list";

        String POINT_INFO_ID = "device_point_info_id";
        String POINT_INFO_PROFILE_INFO_ID = "device_point_info_profile_info_id";
        String POINT_INFO_DIC = "device_point_info_dic";
        String POINT_INFO_LIST = "device_point_info_list";

        String DIC_ID = "device_dic_id";
        String DIC_LABEL_TYPE = "device_dic_label_type";
        String DIC_DIC = "device_dic_dic";
        String DIC_LIST = "device_dic_list";

    }

}
