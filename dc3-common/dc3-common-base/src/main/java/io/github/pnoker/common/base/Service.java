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

package io.github.pnoker.common.base;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 基础服务类接口，业务服务接口请实现该接口
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface Service<T, D> {
    /**
     * 新增
     *
     * @param type Object
     * @return Object
     */
    T add(T type);

    /**
     * 删除
     *
     * @param id ID
     * @return 是否删除
     */
    Boolean delete(String id);

    /**
     * 更新
     *
     * @param type Object
     * @return Object
     */
    T update(T type);

    /**
     * 通过 ID 查询
     *
     * @param id ID
     * @return Object
     */
    T selectById(String id);

    /**
     * 获取带分页、排序
     *
     * @param dto Dto Object
     * @return Page Of Object
     */
    Page<T> list(D dto);

    /**
     * 统一接口 模糊查询构造器
     *
     * @param dto Dto Object
     * @return QueryWrapper
     */
    LambdaQueryWrapper<T> fuzzyQuery(D dto);

}
