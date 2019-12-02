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

package com.pnoker.common.base.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.base.dto.PageInfo;

/**
 * <p>基础 服务类接口
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface BaseService<T> {
    /**
     * 新增记录
     *
     * @param type
     * @return true/false
     */
    boolean add(T type);

    /**
     * 删除记录
     *
     * @param id
     * @return true/false
     */
    boolean delete(Long id);

    /**
     * 更新记录
     *
     * @param type
     * @return true/false
     */
    boolean update(T type);

    /**
     * 获取带分页、排序的记录
     *
     * @param type
     * @param pageInfo pageNum,pageSize
     * @return list
     */
    Page<T> list(T type, PageInfo pageInfo);

    /**
     * 通过ID查询记录
     *
     * @param id
     * @return type
     */
    T selectById(Long id);

    /**
     * 统一接口 模糊查询构造器
     *
     * @param t
     * @return type
     */
    QueryWrapper<T> fuzzyQuery(T t);

    /**
     * 统一接口 排序构造器 & 字段校验
     *
     * @param pageInfo
     * @return list
     */
    Page<T> pagination(PageInfo pageInfo);

}
