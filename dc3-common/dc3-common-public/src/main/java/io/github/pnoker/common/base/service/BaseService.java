/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 基础 Service 类接口
 *
 * @author pnoker
 * @version 2025.6.0
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
