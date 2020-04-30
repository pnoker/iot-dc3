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

package com.dc3.common.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 基础服务类接口
 *
 * @author pnoker
 */
public interface Service<T, D> {
    /**
     * 新增
     *
     * @param type
     * @return
     */
    T add(T type);

    /**
     * 删除
     *
     * @param id
     * @return
     */
    boolean delete(Long id);

    /**
     * 更新
     *
     * @param type
     * @return true/false
     */
    T update(T type);

    /**
     * 通过 ID 查询
     *
     * @param id
     * @return
     */
    T selectById(Long id);

    /**
     * 获取带分页、排序
     *
     * @param dto
     * @return
     */
    Page<T> list(D dto);

    /**
     * 统一接口 模糊查询构造器
     *
     * @param dto
     * @return QueryWrapper
     */
    LambdaQueryWrapper<T> fuzzyQuery(D dto);

}
