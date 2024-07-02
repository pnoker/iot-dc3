/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 基础 Service 类接口
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface BaseService<B, Q> {

    /**
     * <p>
     * 新增
     * </p>
     *
     * @param entityBO Entity of BO
     */
    void save(B entityBO);

    /**
     * <p>
     * 删除
     * </p>
     *
     * @param id Entity ID
     */
    void remove(Long id);

    /**
     * <p>
     * 更新
     * </p>
     *
     * @param entityBO Entity of BO
     */
    void update(B entityBO);

    /**
     * <p>
     * 主键查询
     * </p>
     *
     * @param id Entity ID
     * @return Entity of BO
     */
    B selectById(Long id);

    /**
     * <p>
     * 分页查询
     * </p>
     *
     * @param entityQuery Entity of Query
     * @return Entity of BO Page
     */
    Page<B> selectByPage(Q entityQuery);
}
