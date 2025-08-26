/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.constant.common;

/**
 * 后缀 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class SuffixConstant {

    public static final String ID = "_id";
    public static final String TENANT_ID = "_tenant_id";
    public static final String USER_ID = "_user_id";
    public static final String DRIVER_ID = "_driver_id";
    public static final String GROUP_ID = "_group_id";
    public static final String PROFILE_ID = "_profile_id";
    public static final String POINT_ID = "_point_id";
    public static final String DEVICE_ID = "_device_id";
    public static final String ATTRIBUTE_ID = "_attribute_id";
    public static final String ENTITY_ID = "_entity_id";
    public static final String DEVICE = "_device";
    public static final String PROFILE = "_profile";
    public static final String POINT = "_point";
    public static final String DRIVER_ATTRIBUTE_CONFIG = "_driver_attribute_config";
    public static final String POINT_ATTRIBUTE_CONFIG = "_point_attribute_config";
    public static final String NAME = "_name";
    public static final String PHONE = "_phone";
    public static final String EMAIL = "_email";
    public static final String SALT = "_salt";
    public static final String TOKEN = "_token";
    public static final String LIMIT = "_limit";
    public static final String VALUE = "_value";
    public static final String STATUS = "_status";
    public static final String TYPE = "_type";
    public static final String UNIT = "_unit";
    public static final String IP = "_ip";
    public static final String HOST_PORT = "_host_port";
    public static final String SERVICE_NAME = "_service_name";
    public static final String LIST = "_list";
    public static final String DIC = "_dic";
    public static final String ROLE = "_role";
    public static final String RESOURCE = "_resource";

    private SuffixConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
