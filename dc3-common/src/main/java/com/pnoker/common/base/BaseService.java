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

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author    : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: 基础 服务类接口
 */
public interface BaseService<T> {
    /**
     * 新增记录
     *
     * @param type
     * @return type
     */
    T add(T type);

    /**
     * 删除记录
     *
     * @param id
     * @return true/false
     */
    boolean delete(long id);

    /**
     * 更新记录
     *
     * @param type
     * @return true/false
     */
    T update(T type);

    /**
     * 获取记录
     *
     * @param type
     * @return typeList
     */
    List<T> list(T type);

    /**
     * 通过ID查询记录
     *
     * @param id
     * @return type
     */
    T selectById(long id);
}
