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
 * 通用返回结果枚举
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@AllArgsConstructor
public enum ResponseEnum {
    NO_RESOURCE("R_404", "Resource does not exist");

    /**
     * 返回结果编码
     */
    private final String code;

    /**
     * 返回结果名称
     */
    private final String name;

    /**
     * 通过返回结果编码获取名称
     *
     * @param code 返回结果编码
     * @return PointValueTypeEnum
     */
    public static ResponseEnum getByCode(String code) {
        Optional<ResponseEnum> any = Arrays.stream(ResponseEnum.values()).filter(type -> type.getCode().equals(code)).findAny();
        return any.orElse(null);
    }
}
