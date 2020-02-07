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

        String DC3_MANAGER = "DC3_MANAGER";
        String DC3_MANAGER_DRIVER_URL_PREFIX = "/manager/driver";
        String DC3_MANAGER_CONNECT_INFO_URL_PREFIX = "/manager/connectInfo";
        String DC3_MANAGER_PROFILE_INFO_URL_PREFIX = "/manager/profileInfo";
        String DC3_MANAGER_PROFILE_URL_PREFIX = "/manager/profile";
        String DC3_MANAGER_POINT_URL_PREFIX = "/manager/point";
        String DC3_MANAGER_GROUP_URL_PREFIX = "/manager/group";
        String DC3_MANAGER_DEVICE_URL_PREFIX = "/manager/device";
        String DC3_MANAGER_POINT_INFO_URL_PREFIX = "/manager/pointInfo";
        String DC3_MANAGER_LABEL_URL_PREFIX = "/manager/label";
        String DC3_MANAGER_DIC_URL_PREFIX = "/manager/dic";

        String DC3_RTMP = "DC3-RTMP";
        String DC3_RTMP_URL_PREFIX = "/transfer/rtmp";
    }

    /**
     * 缓存Key
     */
    interface Cache {
        String ID = "_id";
        String NAME = "_name";
        String DIC = "_dic";
        String LIST = "_list";

        String USER = "auth_user";


        String GROUP_ID = "manager_group_id";
        String GROUP_NAME = "manager_group_name";
        String GROUP_DIC = "manager_group_dic";
        String GROUP_LIST = "manager_group_list";

        String DRIVER_ID = "manager_driver_id";
        String DRIVER_NAME = "manager_driver_name";
        String DRIVER_DIC = "manager_driver_dir";
        String DRIVER_LIST = "manager_driver_list";

        String PROFILE_ID = "manager_profile_id";
        String PROFILE_NAME = "manager_profile_name";
        String PROFILE_DIC = "manager_profile_dic";
        String PROFILE_LIST = "manager_profile_list";

        String POINT_ID = "manager_point_id";
        String POINT_NAME = "manager_point_name";
        String POINT_DIC = "manager_point_dic";
        String POINT_LIST = "manager_point_list";

        String DEVICE_ID = "manager_device_id";
        String DEVICE_CODE = "manager_device_code";
        String DEVICE_DIC = "manager_device_dic";
        String DEVICE_GROUP_NAME = "manager_device_group_name";
        String DEVICE_LIST = "manager_device_list";

        String RTMP_ID = "transfer_rtmp_id";
        String RTMP_DIC = "transfer_rtmp_dic";
        String RTMP_LIST = "transfer_rtmp_list";

        String LABEL_ID = "manager_label_id";
        String LABEL_NAME = "manager_label_name";
        String LABEL_DIC = "manager_label_dic";
        String LABEL_LIST = "manager_label_list";

        String LABEL_BIND_ID = "manager_label_bind_id";
        String LABEL_BIND_DIC = "manager_label_bind_dic";
        String LABEL_BIND_LIST = "manager_label_bind_list";

        String CONNECT_INFO_ID = "manager_connect_info_id";
        String CONNECT_INFO_NAME = "manager_connect_info_name";
        String CONNECT_INFO_DIC = "manager_connect_info_dic";
        String CONNECT_INFO_LIST = "manager_connect_info_list";

        String PROFILE_INFO_ID = "manager_profile_info_id";
        String PROFILE_INFO_NAME = "manager_profile_info_name";
        String PROFILE_INFO_DIC = "manager_profile_info_dic";
        String PROFILE_INFO_LIST = "manager_profile_info_list";

        String POINT_INFO_ID = "manager_point_info_id";
        String POINT_INFO_PROFILE_INFO_ID = "manager_point_info_profile_info_id";
        String POINT_INFO_DIC = "manager_point_info_dic";
        String POINT_INFO_LIST = "manager_point_info_list";

        String DIC_ID = "manager_dic_id";
        String DIC_LABEL_TYPE = "manager_dic_label_type";
        String DIC_DIC = "manager_dic_dic";
        String DIC_LIST = "manager_dic_list";

    }

}
