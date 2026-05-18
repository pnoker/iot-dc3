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
 * Generic CRUD service interface.
 *
 * @param <B> Business object type
 * @param <Q> Query object type
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface BaseService<B, Q> {

    /**
     * Add entity to database
     *
     * @param entityBO Business object to add
     */
    void add(B entityBO);

    /**
     * Delete entity by ID
     *
     * @param id Entity ID to delete
     */
    void delete(Long id);

    /**
     * Update existing entity
     *
     * @param entityBO Business object with updated data
     */
    void update(B entityBO);

    /**
     * Select entity by primary key
     *
     * @param id Entity ID to query
     * @return Business object of the entity
     */
    B selectById(Long id);

    /**
     * List entities with pagination
     *
     * @param entityQuery Query object with pagination parameters
     * @return Paginated result of business objects
     */
    Page<B> list(Q entityQuery);

}
