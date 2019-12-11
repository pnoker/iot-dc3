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

package com.pnoker.common.base.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * <p>Token Dto
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Token令牌
     * <br>
     * 第一版：使用 用户名（username） + 公钥（publicKey） + 过期时间（expireTime，毫秒） 的字符串通过'私钥对称加密'生成 Token
     * <br>
     * 第二版：使用 用户名（username） + 公钥（publicKey） + 过期时间（expireTime，毫秒） 的字符串通过'私钥非对称密'生成 Token
     */
    private String token;

    /**
     * 公钥
     */
    private String key;
}
