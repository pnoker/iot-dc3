/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 设备、驱动状态枚举
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /**
     * 注册相关
     */
    REGISTERING("REGISTERING", "注册中"),
    UNREGISTERED("UNREGISTERED", "未注册"),

    /**
     * 运行相关
     */
    ONLINE("ONLINE", "在线"),
    OFFLINE("OFFLINE", "离线"),
    MAINTAIN("MAINTAIN", "维护"),
    FAULT("FAULT", "故障"),
    ;


    /**
     * 状态编码
     */
    private final String code;

    /**
     * 状态名称
     */
    private final String name;

    /**
     * 根据 Code 获取枚举
     *
     * @param code Code
     * @return StatusEnum
     */
    public static StatusEnum of(String code) {
        Optional<StatusEnum> any = Arrays.stream(StatusEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }
}
