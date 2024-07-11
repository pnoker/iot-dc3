/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.manager.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.entity.bo.DictionaryBO;
import io.github.pnoker.common.manager.entity.query.DictionaryQuery;

/**
 * Dictionary Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DictionaryService {

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
