/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.center.manager.service;

import com.dc3.common.bean.Dictionary;

import java.util.List;

/**
 * Dictionary Interface
 *
 * @author pnoker
 */
public interface DictionaryService {

    /**
     * 获取驱动字典
     *
     * @return
     */
    List<Dictionary> driverDictionary();

    /**
     * 获取驱动配置属性字典
     *
     * @return
     */
    List<Dictionary> driverAttributeDictionary();

    /**
     * 获取位号配置属性字典
     *
     * @return
     */
    List<Dictionary> pointAttributeDictionary();

    /**
     * 获取模板字典
     *
     * @return
     */
    List<Dictionary> profileDictionary();

    /**
     * 获取分组字典
     *
     * @return
     */
    List<Dictionary> groupDictionary();

    /**
     * 获取设备字典
     * group/driver/profile
     *
     * @param parent
     * @return
     */
    List<Dictionary> deviceDictionary(String parent);

    /**
     * 获取位号字典
     * driver/profile/device
     *
     * @param parent
     * @return
     */
    List<Dictionary> pointDictionary(String parent);

}
