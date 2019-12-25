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

package com.pnoker.api.center.dbs.token.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.dbs.token.hystrix.TokenDbsFeignClientHystrix;
import com.pnoker.common.bean.Response;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.auth.TokenDto;
import com.pnoker.common.model.auth.Token;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * <p>TokenDbsFeignClient
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@FeignClient(path = Common.Service.DC3_DBS_TOKEN_URL_PREFIX, name = Common.Service.DC3_DBS, fallbackFactory = TokenDbsFeignClientHystrix.class)
public interface TokenDbsFeignClient {

    /**
     * 新增 Token 记录
     *
     * @param token
     * @return Token
     */
    @PostMapping("/add")
    Response<Token> add(@Validated(Insert.class) @RequestBody Token token);

    /**
     * 根据 ID 删除 Token
     *
     * @param id
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    Response<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Token 记录
     *
     * @param token
     * @return Token
     */
    @PostMapping("/update")
    Response<Token> update(@Validated(Update.class) @RequestBody Token token);

    /**
     * 根据 ID 查询 Token
     *
     * @param id
     * @return Token
     */
    @GetMapping("/id/{id}")
    Response<Token> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据 User ID 查询 Token
     *
     * @param id
     * @return Token
     */
    @GetMapping("/user/{id}")
    Response<Token> selectByUserId(@PathVariable(value = "id") Long id);

    /**
     * 分页查询 Token
     *
     * @param tokenDto
     * @return Page<Token>
     */
    @PostMapping("/list")
    Response<Page<Token>> list(@RequestBody(required = false) TokenDto tokenDto);
}
