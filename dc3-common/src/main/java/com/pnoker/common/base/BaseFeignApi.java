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
package com.pnoker.common.base;

import com.pnoker.common.bean.base.Response;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: The class base FeignApi.
 */
public interface BaseFeignApi {

    /**
     * Add 新增操作
     *
     * @param json
     * @return
     */
    @PostMapping(value = "/add")
    Response add(String json);

    /**
     * Delete 删除操作
     *
     * @param json
     * @return
     */
    @DeleteMapping(value = "/delete")
    Response delete(String json);

    /**
     * Update 更新操作
     *
     * @param json
     * @return
     */
    @PutMapping(value = "/update")
    Response update(String json);

    /**
     * List 查询操作
     *
     * @return
     */
    @GetMapping(value = "/list")
    Response list(String json);
}
