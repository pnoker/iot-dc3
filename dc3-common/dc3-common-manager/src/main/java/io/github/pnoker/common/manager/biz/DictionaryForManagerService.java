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

package io.github.pnoker.common.manager.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.dal.entity.bo.DictionaryBO;
import io.github.pnoker.common.manager.entity.query.DictionaryQuery;

/**
 * Dictionary Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public interface DictionaryForManagerService {

    /**
     * Get driver dictionary list with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> driverDictionary(DictionaryQuery entityQuery);

    /**
     * Get profile dictionary list with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> profileDictionary(DictionaryQuery entityQuery);

    /**
     * Get point dictionary under profile with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> pointDictionaryForProfile(DictionaryQuery entityQuery);

    /**
     * Get point dictionary under device with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> pointDictionaryForDevice(DictionaryQuery entityQuery);

    /**
     * Get device dictionary list with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> deviceDictionary(DictionaryQuery entityQuery);

    /**
     * Get device dictionary under driver with pagination
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> deviceDictionaryForDriver(DictionaryQuery entityQuery);

}
