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

package io.github.pnoker.center.manager.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.query.DictionaryQuery;
import io.github.pnoker.common.entity.common.Dictionary;

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
     * @param dictionaryQuery 字典和分页参数
     * @return 带分页的 {@link DictionaryQuery}
     */
    Page<Dictionary> driverDictionary(DictionaryQuery dictionaryQuery);

    /**
     * 获取设备字典带分页的列表
     *
     * @param dictionaryQuery 字典和分页参数
     * @return 带分页的 {@link DictionaryQuery}
     */
    Page<Dictionary> deviceDictionary(DictionaryQuery dictionaryQuery);

    /**
     * 获取模板字典带分页的列表
     *
     * @param dictionaryQuery 字典和分页参数
     * @return 带分页的 {@link DictionaryQuery}
     */
    Page<Dictionary> profileDictionary(DictionaryQuery dictionaryQuery);

    /**
     * 带分页的列表
     * profile/device
     *
     * @param dictionaryQuery 字典和分页参数
     * @return 带分页的 {@link DictionaryQuery}
     */
    Page<Dictionary> pointDictionary(DictionaryQuery dictionaryQuery);

}
