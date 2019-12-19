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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Pages;

/**
 * <p>Service接口
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
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
     * @return
     */
    QueryWrapper<T> fuzzyQuery(D dto);

    /**
     * 统一接口 排序构造器 & 字段校验
     *
     * @param pages
     * @return
     */
    Page<T> pagination(Pages pages);

}
