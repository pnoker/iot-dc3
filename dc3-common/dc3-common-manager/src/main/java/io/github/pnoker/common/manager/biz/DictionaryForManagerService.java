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
 * @version 2025.6.0
 * @since 2022.1.0
 */
public interface DictionaryForManagerService {

    /**
     * 获取驱动字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> driverDictionary(DictionaryQuery entityQuery);

    /**
     * 获取模版字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> profileDictionary(DictionaryQuery entityQuery);

    /**
     * 获取模版下位号字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> pointDictionaryForProfile(DictionaryQuery entityQuery);

    /**
     * 获取设备下位号字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> pointDictionaryForDevice(DictionaryQuery entityQuery);

    /**
     * 获取设备字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> deviceDictionary(DictionaryQuery entityQuery);

    /**
     * 获取驱动下设备字典带分页的列表
     *
     * @param entityQuery {@link DictionaryQuery}
     * @return DictionaryBO Page
     */
    Page<DictionaryBO> deviceDictionaryForDriver(DictionaryQuery entityQuery);

}
