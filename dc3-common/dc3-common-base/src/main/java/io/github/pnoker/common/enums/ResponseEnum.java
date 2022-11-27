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

    OK("R_200", "Success"),
    FAILURE("R_500", "Service exception"),
    NO_RESOURCE("R_404", "Resource does not exist"),
    OUT_RANGE("R_900", "Number out range"),
    ;

    /**
     * 返回结果编码
     */
    private final String code;

    /**
     * 返回结果信息
     */
    private final String message;

    /**
     * 根据 Code 获取枚举
     *
     * @param code Code
     * @return ResponseEnum
     */
    public static ResponseEnum of(String code) {
        Optional<ResponseEnum> any = Arrays.stream(ResponseEnum.values()).filter(type -> type.getCode().equals(code)).findFirst();
        return any.orElse(null);
    }
}
