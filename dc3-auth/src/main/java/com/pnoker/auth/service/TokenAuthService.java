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

package com.pnoker.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.entity.auth.Token;

/**
 * <p>TokenAuthService
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface TokenAuthService {

    /**
     * 新增 Token 记录
     *
     * @param token
     * @return Token
     */
    Response<Token> add(Token token);

    /**
     * 删除 Token 记录
     *
     * @param id
     * @return
     */
    Response<Boolean> delete(Long id);

    /**
     * 更新 Token 记录
     *
     * @param token
     * @return Token
     */
    Response<Token> update(Token token);

    /**
     * 通过 ID 查询 Token 记录
     *
     * @param id
     * @return Token
     */
    Response<Token> selectById(Long id);

    /**
     * 获取带分页、排序的 Token 记录
     *
     * @param tokenDto
     * @return Page<Token>
     */
    Response<Page<Token>> list(TokenDto tokenDto);

    /**
     * 判断 Token 令牌是否有效
     *
     * @param tokenDto
     * @return Boolean
     */
    Response<Boolean> checkTokenValid(TokenDto tokenDto);

}
