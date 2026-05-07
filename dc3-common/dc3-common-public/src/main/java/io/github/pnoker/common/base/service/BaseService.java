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
 * Base Service Interface
 * <p>
 * Generic service interface defining standard CRUD operations for entity management in
 * IoT DC3 platform. Provides common methods for create, read, update, and delete
 * operations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface BaseService<B, Q> {

    /**
     * Save entity to database
     *
     * @param entityBO Business object to save
     */
    void save(B entityBO);

    /**
     * Remove entity by ID
     *
     * @param id Entity ID to remove
     */
    void remove(Long id);

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
     * Select entities with pagination
     *
     * @param entityQuery Query object with pagination parameters
     * @return Paginated result of business objects
     */
    Page<B> selectByPage(Q entityQuery);

}
